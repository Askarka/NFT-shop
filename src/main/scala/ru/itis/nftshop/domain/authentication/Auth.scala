package ru.itis.nftshop.domain.authentication

import cats.{Applicative, MonadError}
import cats.effect._
import ru.itis.nftshop.domain.account.Account
import ru.itis.nftshop.domain.authentication.Role
import org.http4s.Response
import tsec.authentication.{
  AugmentedJWT,
  BackingStore,
  IdentityStore,
  JWTAuthenticator,
  SecuredRequest,
  TSecAuthService,
}
import tsec.authorization.{AuthorizationInfo, BasicRBAC}
import tsec.common.SecureRandomId
import tsec.jws.mac.JWSMacCV
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.mac.jca.MacSigningKey

import scala.concurrent.duration._

object Auth {
  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, Account] =
    new AuthorizationInfo[F, Role, Account] {
      def fetchInfo(u: Account): F[Role] = F.pure(u.role)
    }
  def jwtAuthenticator[F[_]: Sync, Auth: JWTMacAlgo](
      key: MacSigningKey[Auth],
      authRepo: BackingStore[F, SecureRandomId, AugmentedJWT[Auth, Long]],
      userRepo: IdentityStore[F, Long, Account],
  )(implicit cv: JWSMacCV[F, Auth]): JWTAuthenticator[F, Long, Account, Auth] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 1.hour,
      maxIdle = None,
      tokenStore = authRepo,
      identityStore = userRepo,
      signingKey = key,
    )

  private def _allRoles[F[_], Auth](implicit
      F: MonadError[F, Throwable],
  ): BasicRBAC[F, Role, Account, Auth] =
    BasicRBAC.all[F, Role, Account, Auth]

  def allRoles[F[_], Auth](
      pf: PartialFunction[SecuredRequest[F, Account, AugmentedJWT[Auth, Long]], F[Response[F]]],
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[Account, AugmentedJWT[Auth, Long], F] =
    TSecAuthService.withAuthorization(_allRoles[F, AugmentedJWT[Auth, Long]])(pf)

  def allRolesHandler[F[_], Auth](
      pf: PartialFunction[SecuredRequest[F, Account, AugmentedJWT[Auth, Long]], F[Response[F]]],
  )(
      onNotAuthorized: TSecAuthService[Account, AugmentedJWT[Auth, Long], F],
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[Account, AugmentedJWT[Auth, Long], F] =
    TSecAuthService.withAuthorizationHandler(_allRoles[F, AugmentedJWT[Auth, Long]])(
      pf,
      onNotAuthorized.run,
    )
}
