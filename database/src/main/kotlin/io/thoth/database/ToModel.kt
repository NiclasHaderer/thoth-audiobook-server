package io.thoth.database

interface ToModel<T> {
    fun toModel(): T
}
