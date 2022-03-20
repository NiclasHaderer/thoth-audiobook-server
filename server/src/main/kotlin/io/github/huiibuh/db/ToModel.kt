package io.github.huiibuh.db

import io.github.huiibuh.db.tables.*

interface ToModel<T> {
    fun toModel(): T
}

fun removeAllUnusedFromDb() {
    Series.removeUnused()
    Book.removeUnused()
    Author.removeUnused()
    Image.removeUnused()
    ProviderID.removeUnused()
}
