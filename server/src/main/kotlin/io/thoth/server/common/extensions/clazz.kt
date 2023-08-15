package io.thoth.server.common.extensions

@Suppress("UNCHECKED_CAST")
fun <T : Any> Any.getFieldByName(name: String): T {
    return this::class.java.getDeclaredField(name).let { field ->
        field.isAccessible = true
        field.get(this) as T
    }
}
