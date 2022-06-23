package ru.itis.nftshop.infrastructure

import org.http4s.Response
import ru.itis.nftshop.domain.account.Account
import tsec.authentication.{AugmentedJWT, SecuredRequest, TSecAuthService}

package object endpoint {
  type AuthService[F[_], Auth] = TSecAuthService[Account, AugmentedJWT[Auth, Long], F]
  type AuthEndpoint[F[_], Auth] =
    PartialFunction[SecuredRequest[F, Account, AugmentedJWT[Auth, Long]], F[Response[F]]]
}
