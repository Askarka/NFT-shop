package ru.itis.nftshop

import ru.itis.nftshop.config.{DatabaseConfig, NftShopConfig}
import cats.effect._
import cats.effect.ContextShift
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import doobie.util.ExecutionContexts
import io.circe.config.parser
import io.circe.generic.auto._
import ru.itis.nftshop.domain.account.AccountService
import ru.itis.nftshop.domain.authentication.Auth
import ru.itis.nftshop.infrastructure.endpoint.{PropertyEndpoints, UserEndpoints}
import ru.itis.nftshop.infrastructure.repository.doobie.{DoobieAccountRepositoryInterpreter, DoobieAuthRepositoryInterpreter, DoobiePropertyRepositoryInterpreter}
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import ru.itis.nftshop.domain.property.PropertyService



object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, NftShopConfig]("nftshop"))
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      key <- Resource.eval(HMACSHA256.generateKey[F])
      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
      userRepo = DoobieAccountRepositoryInterpreter[F](xa)


      authenticator = Auth.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)

      routeAuth = SecuredRequestHandler(authenticator)
      userService = AccountService[F](userRepo)
      propertyRepo = DoobiePropertyRepositoryInterpreter[F](xa)
      propertyService = PropertyService[F](propertyRepo)
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      httpApp = Router(
              "/acc" -> UserEndpoints
                .endpoints[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
              "/property" -> PropertyEndpoints.endpoints[F, HMACSHA256](propertyService, routeAuth),
      ).orNotFound
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)

}
