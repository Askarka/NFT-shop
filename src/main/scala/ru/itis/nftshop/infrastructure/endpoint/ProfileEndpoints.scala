package ru.itis.nftshop.infrastructure.endpoint

import cats.effect.Sync
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import ru.itis.nftshop.domain.profile.{Profile, ProfileCreationRequest, ProfileService}
import org.http4s.circe._
import io.circe.generic.auto._
import ru.itis.nftshop.infrastructure.repository.doobie.DoobieAccountRepositoryInterpreter





class ProfileEndpoints[F[_]: Sync] extends Http4sDsl[F]{
  implicit val profileDecoder: EntityDecoder[F, Profile] = jsonOf
//  implicit val signupReqDecoder: EntityDecoder[F, ProfileCreationRequest] = jsonOf
//  implicit val
//  private def createProfileEndpoint(profileServece: ProfileService[F]): HttpRoutes[F] = HttpRoutes.of[F]
//  {
//    case req @ POST -> Root / "create" =>
//      val action = for {
//        prof <- req.as[Profile]
//        result <- profileServece.create(prof)
//      } yield result
//  }
}
