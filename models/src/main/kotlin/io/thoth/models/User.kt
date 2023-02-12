package io.thoth.models

import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

interface IUserModel {
    val id: UUID
    val username: String
    val admin: Boolean
    val edit: Boolean
}


@Serializable
data class UserModel(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val username: String,
    override val admin: Boolean,
    override val edit: Boolean
) : IUserModel

@Serializable
data class InternalUserModel(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val username: String,
    override val admin: Boolean,
    override val edit: Boolean,
    val passwordHash: String,
) : IUserModel {
    fun toPublicModel(): UserModel = UserModel(id = id, username = username, admin = admin, edit = edit)
}
