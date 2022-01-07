package io.github.huiibuh.db.tables

import io.github.huiibuh.db.ToModel
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.models.ProviderIDModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object TProviderID : UUIDTable("ProviderID") {
    val provider = char("provider", 40)
    val bookID = char("bookID", 40)
}


class ProviderID(id: EntityID<UUID>) : UUIDEntity(id), ToModel<ProviderIDModel> {
    companion object : UUIDEntityClass<ProviderID>(TProviderID) {

        fun eq(provider: ProviderID?, providerIdCopy: ProviderIDModel?): Boolean {
            return provider?.provider === providerIdCopy?.provider && provider?.itemID === providerIdCopy?.itemID
        }

        fun newFrom(providerID: ProviderWithIDMetadata?): ProviderID? {
            return if (providerID == null) null
            else this.new {
                this.provider = providerID.provider
                this.itemID = providerID.itemID
            }
        }
    }

    var provider by TProviderID.provider
    var itemID by TProviderID.bookID

    override fun toModel() = ProviderIDModel(
        provider = this.provider,
        itemID = this.itemID
    )

}
