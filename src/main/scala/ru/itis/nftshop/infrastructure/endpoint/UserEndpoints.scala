package ru.itis.nftshop.infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.{EntityDecoder, HttpRoutes, QueryParamDecoder}
import org.jsoup.Connection.Base
import tsec.common.Verified
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.passwordhashers.{PasswordHash, PasswordHasher}
import tsec.authentication._
import ru.itis.nftshop.domain.account._
import ru.itis.nftshop.domain.authentication.{Auth, LoginRequest, SignupRequest}
import ru.itis.nftshop.domain.{UserAccountError, UserError, UserNameError}

class UserEndpoints[F[_]: Sync, A, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  /* Necessary for decoding query parameters */
  import QueryParamDecoder._

  /* Parses out the optional offset and page size params */
  object OptionalPageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("pageSize")
  object OptionalOffsetMatcher extends OptionalQueryParamDecoderMatcher[Int]("offset")

  implicit val userDecoder: EntityDecoder[F, Account] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf

  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  private def loginEndpoint(
      userService: AccountService[F],
      cryptService: PasswordHasher[F, A],
      auth: Authenticator[F, Long, Account, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
      val action = for {
        login <- EitherT.liftF(req.as[LoginRequest])
        name = login.nickname
        user <- userService.getAccountByNickname(name).leftMap(_ => UserNameError(name))
        checkResult <- EitherT.liftF(
          cryptService.checkpw(login.password, PasswordHash[A](user.passwordHash)),
        )
        _ <-
          if (checkResult == Verified) EitherT.rightT[F, UserNameError](())
          else EitherT.leftT[F, Account](UserNameError(name))
        token <- user.accountId match {
          case None => throw new Exception("Impossible") // User is not properly modeled
          case Some(id) => EitherT.right[UserNameError](auth.create(id))
        }
      } yield (user, token)

      action.value.flatMap {
        case Right((user, token)) => Ok(user.asJson).map(auth.embed(_, token))
        case Left(UserNameError(name)) =>
          BadRequest(s"Authentication failed for user $name")
      }
    }

  private def signupEndpoint(
      userService: AccountService[F],
      crypt: PasswordHasher[F, A],
  ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / "signUp" =>
      val action = for {
        signup <- req.as[SignupRequest]
        hash <- crypt.hashpw(signup.password)
        user <- signup.asAccount(hash).pure[F]
        result <- userService.createAccount(user).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(_) =>
          Conflict(s"The user with this name already exists")
      }
    }

  private def updateEndpoint(userService: AccountService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ PUT -> Root / name =>
      val action = for {
        user <- req.as[Account]
        updated = user.copy(nickname = name)
        result <- userService.update(updated).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserError) => NotFound("User not found")
      }
    }

  private def listEndpoint(userService: AccountService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
          offset,
        )  =>
      for {
        retrieved <- userService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def searchByNameEndpoint(userService: AccountService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / userName asAuthed _ =>
      userService.getAccountByNickname(userName).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(UserError) => NotFound("The user was not found")
      }
  }

  private def deleteUserEndpoint(userService: AccountService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / userName asAuthed _ =>
      for {
        _ <- userService.deleteByAccountNickname(userName)
        resp <- Ok()
      } yield resp
  }

  def endpoints(
      userService: AccountService[F],
      cryptService: PasswordHasher[F, A],
      auth: SecuredRequestHandler[F, Long, Account, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] =
      Auth.allRoles {

          searchByNameEndpoint(userService)
          .orElse(deleteUserEndpoint(userService))
      }

    val unauthEndpoints =
      loginEndpoint(userService, cryptService, auth.authenticator) <+>
        signupEndpoint(userService, cryptService) <+>
        updateEndpoint(userService) <+>
        listEndpoint(userService)


    unauthEndpoints <+> auth.liftService(authEndpoints)
  }
}

object UserEndpoints {
  def endpoints[F[_]: Sync, A, Auth: JWTMacAlgo](
      userService: AccountService[F],
      cryptService: PasswordHasher[F, A],
      auth: SecuredRequestHandler[F, Long, Account, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] =
    new UserEndpoints[F, A, Auth].endpoints(userService, cryptService, auth)
}
