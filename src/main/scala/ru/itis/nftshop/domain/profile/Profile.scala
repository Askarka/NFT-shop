package ru.itis.nftshop.domain.profile

import cats.implicits.{catsSyntaxOptionId, toFlatMapOps, toFunctorOps}
import cats.{Functor, Monad}
import org.h2.engine.User
import ru.itis.nftshop.domain.account.{AccountRepositoryAlgebra, AccountService}
import ru.itis.nftshop.infrastructure.repository.doobie.DoobieAccountRepositoryInterpreter

case class Profile(
    profileId: Option[Long] = None,
    accountId: Option[Long],
    firstName: String,
    surname: String,
    gender: String,
)

final case class ProfileCreationRequest(
    accountName: Option[Long],
    firstName: String,
    surname: String,
    gender: String,
    userName: String
) {
  def toProfile[Option[_]: Monad](accountRepository: AccountRepositoryAlgebra[Option]): Profile = Profile(
//      accountId = accountRepository.findByAccountNickname(userName).value.???,
      accountId = ???,
      firstName = firstName,
      surname = surname,
      gender = gender
  )
}
