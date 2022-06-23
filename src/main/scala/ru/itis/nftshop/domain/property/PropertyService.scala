package ru.itis.nftshop.domain.property

import cats.{Functor, Monad}
import cats.data.{EitherT, OptionT}
import cats.syntax.all._
import ru.itis.nftshop.domain.PropertyNotFoundError

class PropertyService[F[_]](repository: PropertyRepositoryAlgebra[F]) {
  def create(property: Property)(implicit M: Monad[F]): OptionT[F, Property] =
    OptionT.liftF(repository.create(property))

  def update(property: Property)(implicit
      M: Monad[F],
  ): EitherT[F, PropertyNotFoundError.type, Property] =
    EitherT.fromOptionF(repository.update(property), PropertyNotFoundError)

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, PropertyNotFoundError.type, Property] =
    EitherT.fromOptionF(repository.get(id), PropertyNotFoundError)

  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    repository.delete(id).as(())

  def list(pageSize: Int, offset: Int): F[List[Property]] =
    repository.list(pageSize, offset)
}

object PropertyService {
  def apply[F[_]](
      repository: PropertyRepositoryAlgebra[F],
  ): PropertyService[F] = new PropertyService[F](repository)
}
