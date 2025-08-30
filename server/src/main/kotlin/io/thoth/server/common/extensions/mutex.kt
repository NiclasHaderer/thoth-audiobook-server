package io.thoth.server.common.extensions

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> Mutex.withGuard(
    owner: Any? = null,
    action: () -> T,
): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    runBlocking { lock(owner) }
    return try {
        action()
    } finally {
        unlock(owner)
    }
}
