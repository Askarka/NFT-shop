package ru.itis.nftshop.domain.account

import cats.data.OptionT

trait AccountRepositoryAlgebra[F[_]] {
  def create(account: Account): F[Account]

  def update(account: Account): OptionT[F, Account]

  def get(id: Long): OptionT[F, Account]

  def delete(id: Long): OptionT[F, Account]

  def findByAccountNickname(accountNickname: String): OptionT[F, Account]

  def deleteByAccountNickname(accountNickname: String): OptionT[F, Account]

  def list(pageSize: Int, offset: Int): F[List[Account]]
}
