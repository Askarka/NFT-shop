package ru.itis.nftshop.domain

import ru.itis.nftshop.domain.account.Account

sealed trait ValidationError extends Product with Serializable
case object UserError extends ValidationError
case class UserNameError(name: String) extends ValidationError
case class UserAccountError(user: Account) extends ValidationError
case object ProfileNotFoundError extends ValidationError
case object PropertyNotFoundError extends ValidationError