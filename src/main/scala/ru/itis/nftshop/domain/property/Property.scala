package ru.itis.nftshop.domain.property

case class Property (
    propertyId: Option[Long] = None,
    accountId: Option[Long],
    tokenHash: String,
    description: String
)
