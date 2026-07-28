package io.thoth.metadata.audible.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudibleApiProductResponse(
    val product: AudibleApiProduct? = null,
)

@Serializable
data class AudibleApiProductsResponse(
    val products: List<AudibleApiProduct> = emptyList(),
)

@Serializable
data class AudibleApiProduct(
    val asin: String,
    val title: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val authors: List<AudibleApiPerson> = emptyList(),
    val narrators: List<AudibleApiPerson> = emptyList(),
    val series: List<AudibleApiSeries> = emptyList(),
    val rating: AudibleApiRating? = null,
    val relationships: List<AudibleApiRelationship> = emptyList(),
    @SerialName("content_delivery_type") val contentDeliveryType: String? = null,
    @SerialName("publisher_name") val publisherName: String? = null,
    @SerialName("publisher_summary") val publisherSummary: String? = null,
    @SerialName("merchandising_summary") val merchandisingSummary: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("product_images") val productImages: Map<String, String> = emptyMap(),
)

@Serializable
data class AudibleApiPerson(
    val asin: String? = null,
    val name: String? = null,
)

@Serializable
data class AudibleApiSeries(
    val asin: String? = null,
    val title: String? = null,
    val sequence: String? = null,
)

@Serializable
data class AudibleApiRating(
    @SerialName("overall_distribution") val overallDistribution: AudibleApiRatingDistribution? = null,
)

@Serializable
data class AudibleApiRatingDistribution(
    @SerialName("average_rating") val averageRating: Float? = null,
)

@Serializable
data class AudibleApiRelationship(
    val asin: String? = null,
    val sequence: String? = null,
    @SerialName("relationship_to_product") val relationshipToProduct: String? = null,
    @SerialName("relationship_type") val relationshipType: String? = null,
)
