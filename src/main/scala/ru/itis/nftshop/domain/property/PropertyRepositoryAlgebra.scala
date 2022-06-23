package ru.itis.nftshop.domain.property

import cats.data.OptionT
import com.openhtmltopdf.css.parser.property.PageSize
import jnr.ffi.Struct.Offset

trait PropertyRepositoryAlgebra[F[_]] {
  def create(property: Property): F[Property]

  def get(id: Long): F[Option[Property]]

  def delete(id: Long): F[Option[Property]]

  def update(property: Property): F[Option[Property]]

  def list(pageSize: Int, offset: Int): F[List[Property]]
}
