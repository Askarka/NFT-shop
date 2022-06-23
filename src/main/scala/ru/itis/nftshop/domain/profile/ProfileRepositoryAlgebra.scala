package ru.itis.nftshop.domain.profile

import cats.data.NonEmptyList

trait ProfileRepositoryAlgebra[F[_]] {
  def create(profile: Profile): F[Profile]

  def update(profile: Profile): F[Option[Profile]]

  def get(id: Long): F[Option[Profile]]

  def delete(id: Long): F[Option[Profile]]

  def list(pageSize: Int, offset: Int): F[List[Profile]]

}