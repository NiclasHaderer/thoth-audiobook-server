package io.github.huiibuh.db.tables

interface ToModel<T> {
    fun toModel(): T
}
