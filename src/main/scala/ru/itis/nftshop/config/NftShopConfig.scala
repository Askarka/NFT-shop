package ru.itis.nftshop.config

final case class ServerConfig(host: String, port: Int)
final case class NftShopConfig(db: DatabaseConfig, server: ServerConfig)
