package ru.itis.nftshop.infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.Auth
import ru.itis.nftshop.domain.PropertyNotFoundError
import ru.itis.nftshop.domain.account.Account
import ru.itis.nftshop.domain.authentication.Auth
import ru.itis.nftshop.domain.property.{Property, PropertyService}
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler, asAuthed}
import tsec.jwt.algorithms.JWTMacAlgo

class PropertyEndpoints[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  implicit val propertyDecoder: EntityDecoder[F, Property] = jsonOf

  private def createProperty(propertyService: PropertyService[F]): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root / "create" asAuthed _ =>
      for {
        property <- req.request.as[Property]
        res <- propertyService.create(property).value
        resp <- Ok(res.asJson)
      } yield resp
  }

  private def getPropertyEndpoint(propertyService: PropertyService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / LongVar(id) asAuthed _ =>
      propertyService.get(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(PropertyNotFoundError) => NotFound("The property was not found")
      }
  }

  private def deleteProperty(propertyService: PropertyService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- propertyService.delete(id)
        resp <- Ok()
      } yield resp
  }

  def endpoints(
                 propertyService: PropertyService[F],
                 auth: SecuredRequestHandler[F, Long, Account, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] =
      Auth.allRolesHandler(
        createProperty(propertyService).orElse(getPropertyEndpoint(propertyService)),
      ) {
        Auth.allRoles(deleteProperty(propertyService))
      }

    auth.liftService(authEndpoints)
  }
}

object PropertyEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](
      propertyService: PropertyService[F],
      auth: SecuredRequestHandler[F, Long, Account, AugmentedJWT[Auth, Long]]): HttpRoutes[F] =
    new PropertyEndpoints[F, Auth].endpoints(propertyService, auth)

}
