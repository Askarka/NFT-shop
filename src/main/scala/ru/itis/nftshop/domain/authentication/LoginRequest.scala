package ru.itis.nftshop.domain.authentication

import ru.itis.nftshop.domain.account.Account
import ru.itis.nftshop.domain.authentication.Role
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(
    nickname: String,
    password: String,
)

final case class SignupRequest(
    nickname: String,
    email: String,
    phoneNumber: String,
    password: String,
    role: Role,
) {
  def asAccount[A](hashedPassword: PasswordHash[A]): Account = Account(
    nickname,
    email,
    phoneNumber,
    hashedPassword,
    role = role,
  )
}
