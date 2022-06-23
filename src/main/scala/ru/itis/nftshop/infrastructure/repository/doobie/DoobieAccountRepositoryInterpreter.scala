package ru.itis.nftshop.infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.Bracket
import cats.implicits.catsSyntaxOptionId
import doobie._
import doobie.implicits.toSqlInterpolator
import doobie.util.update.Update0
import ru.itis.nftshop.domain.account.{Account, AccountRepositoryAlgebra}
import tsec.authentication.IdentityStore
import doobie.implicits._
import cats.syntax.all._
import ru.itis.nftshop.infrastructure.repository.doobie.SQLPagination.paginate

private object UserSQL {
  def insert(account: Account): Update0 = sql"""
       INSERT INTO account(nickname, email, phone_number, password_hash, role)
       VALUES (${account.nickname}, ${account.email}, ${account.phoneNumber}, ${account.passwordHash}, ${account.role})
         """.update

  def select(userId: Long): Query0[Account] = sql"""
        SELECT nickname, email, phone_number, password_hash, account_id, role
        FROM account
        WHERE account_id = $userId
       """.query[Account]

  def update(user: Account, id: Long): Update0 = sql"""
        UPDATE account
        SET nickname = ${user.nickname}, email = ${user.email}, phone_number = ${user.phoneNumber}, password_hash = ${user.passwordHash}, role = ${user.role}
        WHERE account_id = $id
       """.update

  def delete(userId: Long): Update0 =
    sql"""
        DELETE FROM account WHERE account_id = $userId
       """.update

  def byAccountNickname(accountNickname: String): Query0[Account] =
    sql"""
        SELECT nickname, email, phone_number, password_hash, account_id, role
        FROM account
        WHERE nickname = $accountNickname
       """.query[Account]

  def selectAll: Query0[Account] =
    sql"""
        SELECT nickname, email, phone_number, password_hash, account_id, role
        FROM account
       """.query
}
class DoobieAccountRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends AccountRepositoryAlgebra[F]
    with IdentityStore[F, Long, Account] { self =>
  import UserSQL._

  def create(user: Account): F[Account] =
    insert(user)
      .withUniqueGeneratedKeys[Long]("account_id")
      .map(id => user.copy(accountId = id.some))
      .transact(xa)

  def update(user: Account): OptionT[F, Account] =
    OptionT.fromOption[F](user.accountId).semiflatMap { id =>
      UserSQL.update(user, id).run.transact(xa).as(user)
    }

  def get(userId: Long): OptionT[F, Account] = OptionT(select(userId).option.transact(xa))

  def findByAccountNickname(userName: String): OptionT[F, Account] =
    OptionT(byAccountNickname(userName).option.transact(xa))

  def delete(userId: Long): OptionT[F, Account] =
    get(userId).semiflatMap(user => UserSQL.delete(userId).run.transact(xa).as(user))

  def deleteByAccountNickname(userName: String): OptionT[F, Account] =
    findByAccountNickname(userName).mapFilter(_.accountId).flatMap(delete)

  def list(pageSize: Int, offset: Int): F[List[Account]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)

}

object DoobieAccountRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](
      xa: Transactor[F],
  ): DoobieAccountRepositoryInterpreter[F] =
    new DoobieAccountRepositoryInterpreter(xa)
}
