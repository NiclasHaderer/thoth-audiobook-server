package io.github.huiibuh.db.tables

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.db.ToModel
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.models.ProviderIDModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


object TProviderID : UUIDTable("ProviderID") {
    val provider = char("provider", 40)
    val itemID = char("bookID", 40)
}


class ProviderID(id: EntityID<UUID>) : UUIDEntity(id), ToModel<ProviderIDModel> {
    companion object : UUIDEntityClass<ProviderID>(TProviderID) {

        fun eq(provider: ProviderID?, providerIdCopy: ProviderIDModel?): Boolean {
            return provider?.provider === providerIdCopy?.provider && provider?.itemID === providerIdCopy?.itemID
        }

        fun getById(uuid: UUID) = transaction {
            findById(uuid)?.toModel() ?: throw APINotFound("Could not find provider")
        }

        fun getOrCreate(providerID: ProviderWithIDMetadata?): ProviderID? = transaction {
            if (providerID == null) return@transaction null

            find { TProviderID.itemID eq providerID.provider and (TProviderID.provider eq providerID.provider) }.firstOrNull()
                ?: this@Companion.new {
                    this.provider = providerID.provider
                    this.itemID = providerID.itemID
                }
        }

        fun removeUnused() = transaction {
            all().forEach {
                val id = it.id.value
                if (
                    Author.find { TAuthors.providerID eq id }.empty() &&
                    Book.find { TBooks.providerID eq id }.empty() &&
                    Series.find { TSeries.providerID eq id }.empty()
                ) {
                    it.delete()
                }
            }
        }
    }

    var provider by TProviderID.provider
    var itemID by TProviderID.itemID

    override fun toModel() = ProviderIDModel(
        provider = this.provider,
        itemID = this.itemID
    )

}
