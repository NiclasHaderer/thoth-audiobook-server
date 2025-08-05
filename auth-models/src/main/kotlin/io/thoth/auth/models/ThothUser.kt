package io.thoth.auth.models

import java.util.UUID

// User models which will be returned by the auth api

open class ThothUser(open val id: UUID, open val username: String)
open class ThothUserWithPermissions<PERMISSIONS: Any>(open val id: UUID, open val username: String, open val permissions: PERMISSIONS)
