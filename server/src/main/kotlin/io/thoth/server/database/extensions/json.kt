package io.thoth.server.database.extensions

import io.thoth.server.common.extensions.get
import io.thoth.server.di.serialization.Serialization
import io.thoth.server.di.serialization.deserializeValue
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json

inline fun <reified T : Any> Table.json(
    columnName: String,
    crossinline validation: (T) -> Unit = {},
): Column<T> {
    val serializer = get<Serialization>()

    return json(columnName, serialize = {
        validation(it)
        serializer.serializeValue(it)
    }, deserialize = {
        serializer.deserializeValue<T>(it)
    })
}
