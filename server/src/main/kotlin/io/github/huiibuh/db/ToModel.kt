package io.github.huiibuh.db

import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.ProviderID
import io.github.huiibuh.db.tables.Series

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
