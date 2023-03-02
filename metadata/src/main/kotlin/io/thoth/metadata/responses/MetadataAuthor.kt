package io.thoth.metadata.responses

import java.time.LocalDate

interface MetadataSearchAuthor {
  val id: MetadataProviderWithID
  val name: String?
  val link: String
}

data class MetadataSearchAuthorImpl(
    override val id: MetadataProviderWithID,
    override val name: String?,
    override val link: String,
) : MetadataSearchAuthor

interface MetadataAuthor : MetadataSearchAuthor {
  val imageURL: String?
  val biography: String?
  val website: String?
  val bornIn: String?
  val birthDate: LocalDate?
  val deathDate: LocalDate?
}

data class MetadataAuthorImpl(
    override val id: MetadataProviderWithID,
    override val name: String?,
    override val link: String,
    override val imageURL: String?,
    override val biography: String?,
    override val website: String?,
    override val bornIn: String?,
    override val birthDate: LocalDate?,
    override val deathDate: LocalDate?,
) : MetadataAuthor
