package ru.itis.nftshop.domain.account

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.syntax.functor._
import ru.itis.nftshop.domain.UserError

class AccountService[F[_]](userRepo: AccountRepositoryAlgebra[F]) {
  def createAccount(account: Account)(implicit M: Monad[F]): EitherT[F, UserError.type, Account] =
    for {
      saved <- EitherT.liftF(userRepo.create(account))
    } yield saved

  def getAccount(accountId: Long)(implicit F: Functor[F]): EitherT[F, UserError.type, Account] =
    userRepo.get(accountId).toRight(UserError)

  def getAccountByNickname(nickname: String)(implicit F: Functor[F]): EitherT[F, UserError.type, Account] =
    userRepo.findByAccountNickname(nickname).toRight(UserError)

  def deleteUser(userId: Long)(implicit F: Functor[F]): F[Unit] =
    userRepo.delete(userId).value.void

  def deleteByAccountNickname(userName: String)(implicit F: Functor[F]): F[Unit] =
    userRepo.deleteByAccountNickname(userName).value.void

  def update(user: Account)(implicit M: Monad[F]):EitherT[F, UserError.type, Account] =
    for {
      saved <- userRepo.update(user).toRight(UserError)
    } yield saved

  def list(pageSize: Int, offset: Int): F[List[Account]] =
    userRepo.list(pageSize,offset)
}
object AccountService {
  def apply[F[_]](
      repository: AccountRepositoryAlgebra[F],
  ): AccountService[F] =
    new AccountService[F](repository)
}
