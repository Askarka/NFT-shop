package ru.itis.nftshop.infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._
import ru.itis.nftshop.domain.property.{Property, PropertyRepositoryAlgebra}
import ru.itis.nftshop.infrastructure.repository.doobie.SQLPagination.paginate

private object PropertySQL {

  def insert(property: Property): Update0 =
    sql"""
      INSERT INTO property (account_id, token_hash, description)
      VALUES (${property.accountId}, ${property.tokenHash}, ${property.description})
       """.update

  def select(id: Long): Query0[Property] =
    sql"""
       SELECT property_id, account_id, token_hash, description
        FROM property
        WHERE property_id = $id
       """.query[Property]

  def update(property: Property, id: Long): Update0 =
    sql"""
         UPDATE property
        SET account_id = ${property.accountId}, token_hash = ${property.tokenHash}, description = ${property.tokenHash}
       """.update

  def delete(id: Long): Update0 =
    sql"""
         DELETE FROM property
         WHERE property_id = $id
       """.update

  def selectAll: Query0[Property] =
    sql"""
         SELECT property_id, account_id, token_hash, description
        FROM property
        ORDER BY property_id
         """.query
}

class DoobiePropertyRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends PropertyRepositoryAlgebra[F] {

  override def create(property: Property): F[Property] =
    PropertySQL
      .insert(property)
      .withUniqueGeneratedKeys[Long]("property_id")
      .map(id => property.copy(propertyId = id.some))
      .transact(xa)

  override def get(id: Long): F[Option[Property]] =
    PropertySQL.select(id).option.transact(xa)

  override def update(property: Property): F[Option[Property]] =
    OptionT
      .fromOption[ConnectionIO](property.propertyId)
      .semiflatMap(id => PropertySQL.update(property, id).run.as(property))
      .value
      .transact(xa)

  override def delete(id: Long): F[Option[Property]] =
    OptionT(PropertySQL.select(id).option)
      .semiflatMap(property => PropertySQL.delete(id).run.as(property))
      .value
      .transact(xa)

  override def list(pageSize: Int, offset: Int): F[List[Property]] =
    paginate(pageSize, offset)(PropertySQL.selectAll).to[List].transact(xa)
}

object DoobiePropertyRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](
      xa: Transactor[F],
  ): DoobiePropertyRepositoryInterpreter[F] =
    new DoobiePropertyRepositoryInterpreter(xa)
}
