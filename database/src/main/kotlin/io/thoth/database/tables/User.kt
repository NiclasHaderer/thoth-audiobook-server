package io.thoth.database.tables

import java.util.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable

object TUsers : UUIDTable("Users") {
  val username = char("username", 256).uniqueIndex()
  val passwordHash = char("passwordHash", 512)
  val admin = bool("admin")
  val edit = bool("edit")
  val changePassword = bool("changePassword")
  val enabled = bool("enabled").default(true)
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
  companion object : UUIDEntityClass<User>(TUsers)

  var username by TUsers.username
  var passwordHash by TUsers.passwordHash
  var admin by TUsers.admin
  var edit by TUsers.edit
  var changePassword by TUsers.changePassword
  var enabled by TUsers.enabled
}
