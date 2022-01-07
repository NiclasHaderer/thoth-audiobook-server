package io.github.huiibuh.extensions


fun <T> List<T>.saveTo(index: Int): List<T> {
    val searchIndex = if (this.size < index) this.size else index
    return this.subList(0, searchIndex)
}
