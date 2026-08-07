package io.thoth.auth

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val userMutationLock = ReentrantLock()

// Serializes every read-check-write over the user table, so "is this the last admin" and "is this the first
// user" cannot be answered by two requests at once and then both acted on.
internal fun <T> withUserMutation(block: () -> T): T = userMutationLock.withLock(block)
