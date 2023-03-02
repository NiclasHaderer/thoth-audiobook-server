package io.thoth.metadata.responses

interface MetadataProviderWithID {
  val provider: String
  val itemID: String
}

data class MetadataProviderWithIDImpl(override val provider: String, override val itemID: String) :
    MetadataProviderWithID
