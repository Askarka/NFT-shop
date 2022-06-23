package ru.itis.nftshop.domain.account

import ru.itis.nftshop.domain.authentication.Role

case class Account(
    nickname: String,
    email: String,
    phoneNumber: String,
    passwordHash: String,
    accountId: Option[Long] = None,
    role: Role
)
