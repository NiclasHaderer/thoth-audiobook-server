package io.thoth.metadata.responses

interface MetadataSearchAuthor {
    val id: MetadataProviderWithID
    val name: String?
    val link: String
}

class MetadataSearchAuthorImpl(
    override val id: MetadataProviderWithID,
    override val name: String?,
    override val link: String,
) : MetadataSearchAuthor

interface MetadataAuthor : MetadataSearchAuthor {
    val image: String?
    val biography: String?
}

class MetadataAuthorImpl(
    override val id: MetadataProviderWithID,
    override val name: String?,
    override val link: String,
    override val image: String?,
    override val biography: String?,
) : MetadataAuthor