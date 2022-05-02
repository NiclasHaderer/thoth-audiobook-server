package io.thoth.common.extensions


fun <T> List<T>.saveTo(index: Int): List<T> {
    val searchIndex = if (this.size < index) this.size else index
    return this.subList(0, searchIndex)
}
