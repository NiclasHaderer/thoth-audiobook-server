package io.github.huiibuh.utils

import kotlinx.coroutines.runBlocking
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder


fun <E : UUIDEntity> UUIDEntityClass<E>.findOne(op: SqlExpressionBuilder.() -> Op<Boolean>): E? {
    val res = this.find(op)
    if (res.empty()) return null
    return res.firstOrNull()
}


fun <T> List<T>.to(index: Int): List<T> {
    val searchIndex = if (this.size < index) this.size else index
    return this.subList(0, searchIndex)
}

class FuzzyResult<T>(
    val match: Int,
    val value: T,
) {
    val matches: Boolean
        get() = match > 60

}


fun <T> String.fuzzy(query: String, value: T): FuzzyResult<T> {
    val fuz = FuzzySearch.partialRatio(query, this)
    return FuzzyResult(fuz, value)
}

fun <T> SizedIterable<T>.fuzzy(query: String, getValues: (T) -> List<String>): List<T> = this.mapNotNull { item ->
    getValues(item).map { it.fuzzy(query, item) }.maxByOrNull { it.match }
}.filter { it.matches }.sortedByDescending { it.match }.map { it.value }


fun String.uriToFile(): ByteArray = runBlocking {
    imageFromString(this@uriToFile)
}
