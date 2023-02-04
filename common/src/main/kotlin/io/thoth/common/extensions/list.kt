package io.thoth.common.extensions


fun <T> List<T>.saveTo(index: Int): List<T> {
    val searchIndex = if (this.size < index) this.size else index
    return this.subList(0, searchIndex)
}

fun <T> List<T>.saveSubList(startIndex: Int, endIndex: Int? = null): List<T> {
    val searchStartIndex = if (this.size < startIndex) this.size else startIndex
    val searchEndIndex = if (endIndex == null) this.size else if (this.size < endIndex) this.size else endIndex
    return this.subList(searchStartIndex, searchEndIndex)
}

fun <T> List<T>.tap(executor: (it: T) -> Unit): List<T> {
    this.forEach(executor)
    return this
}

fun <T, V> ListIterator<T>.map(transform: (T) -> V): List<V> {
    val list = mutableListOf<V>()
    this.forEach { list.add(transform(it)) }
    return list
}

fun <T, V> ListIterator<T>.mapIndexed(transform: (index: Int, T) -> V): List<V> {
    val list = mutableListOf<V>()
    while (this.hasNext()) {
        val nextIndex = this.nextIndex()
        list.add(transform(nextIndex, this.next()))
    }
    return list
}