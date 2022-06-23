package ru.itis.nftshop.infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.Bracket
import doobie.Transactor
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import ru.itis.nftshop.domain.profile.{Profile, ProfileRepositoryAlgebra}
import cats.syntax.all._
import cats.data._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import SQLPagination._
import cats.effect.Bracket

private object ProfileSQL {
  def insert(profile: Profile): Update0 =
    sql"""
        INSERT INTO profile (account_id, first_name, surname, gender)
        VALUES (${profile.accountId}, ${profile.firstName}, ${profile.surname}, ${profile.gender})
         """.update

  def select(id: Long): Query0[Profile] =
    sql"""
        SELECT profile_id, account_id, first_name, surname, gender
        FROM profile
        WHERE profile_id = $id
       """.query

  def update(profile: Profile, id: Long): Update0 =
    sql"""
        UPDATE profile
        SET account_id = ${profile.accountId}, first_name = ${profile.firstName}, surname = ${profile.surname}, gender = ${profile.gender}
        WHERE profile_id = $id
       """.update

  def delete(id: Long): Update0 =
    sql"""
        DELETE FROM profile WHERE profile_id = $id
       """.update

  def selectAll: Query0[Profile] =
    sql"""
        SELECT profile_id, account_id, first_name, surname, gender
        FROM profile
        ORDER BY profile_id
       """.query
}
class DoobieProfileRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends ProfileRepositoryAlgebra[F] {
  import ProfileSQL._

  override def create(profile: Profile): F[Profile] =
    insert(profile)
      .withUniqueGeneratedKeys[Long]("profile_id")
      .map(id => profile.copy(profileId = id.some))
      .transact(xa)

  override def update(profile: Profile): F[Option[Profile]] =
    OptionT
      .fromOption[ConnectionIO](profile.profileId)
      .semiflatMap(id => ProfileSQL.update(profile, id).run.as(profile))
      .value
      .transact(xa)

  override def get(id: Long): F[Option[Profile]] = select(id).option.transact(xa)

  override def delete(id: Long): F[Option[Profile]] =
    OptionT(select(id).option)
      .semiflatMap(profile => ProfileSQL.delete(id).run.as(profile))
      .value
      .transact(xa)

  override def list(pageSize: Int, offset: Int): F[List[Profile]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)
}

object DoobieProfileRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](
      xa: Transactor[F],
  ): DoobieProfileRepositoryInterpreter[F] =
    new DoobieProfileRepositoryInterpreter(xa)
}
