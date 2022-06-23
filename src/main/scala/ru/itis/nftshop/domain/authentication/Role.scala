package ru.itis.nftshop.domain.authentication

import cats._
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

final case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {
  val base: Role = Role("Base")

  override def getRepr(t: Role): String = t.roleRepr

  override protected val values: AuthGroup[Role] = AuthGroup(base)

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
