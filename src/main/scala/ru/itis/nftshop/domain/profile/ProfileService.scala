package ru.itis.nftshop.domain.profile

import cats.{Functor, Monad}
import cats.data.{EitherT, OptionT}
import cats.syntax.all._
import ru.itis.nftshop.domain._

class ProfileService[F[_]](repository: ProfileRepositoryAlgebra[F]) {
  def create(profile: Profile)(implicit M: Monad[F]): OptionT[F, Profile] =
    OptionT.liftF(repository.create(profile))

  def update(profile: Profile)(implicit
      M: Monad[F],
  ): EitherT[F, ProfileNotFoundError.type, Profile] =
    EitherT.fromOptionF(repository.update(profile), ProfileNotFoundError)

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, ProfileNotFoundError.type, Profile] =
    EitherT.fromOptionF(repository.get(id), ProfileNotFoundError)

  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    repository.delete(id).as(())

  def list(pageSize: Int, offset: Int): F[List[Profile]] =
    repository.list(pageSize, offset)
}

object ProfileService {
  def apply[F[_]](
      repository: ProfileRepositoryAlgebra[F],
  ): ProfileService[F] = new ProfileService[F](repository)
}
