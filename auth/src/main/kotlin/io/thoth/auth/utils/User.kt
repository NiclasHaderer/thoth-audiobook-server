package io.thoth.auth.utils

import io.thoth.auth.models.ThothDatabaseUser
import io.thoth.auth.models.ThothUser

fun ThothDatabaseUser.wrap(): ThothUser {
    return ThothUser(id = this.id, username = this.username)
}
