package io.thoth.server.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.thoth.openapi.common.Patch
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorField
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.thothServer
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

data class Wire(
    val required: Patch<String> = Patch.Absent,
    val nullable: Patch<String?> = Patch.Absent,
    val plain: String?,
)

data class Tagged(
    val tags: Patch<List<String>> = Patch.Absent,
)

data class Scalars(
    val count: Int,
    val flag: Boolean,
    val name: String,
    val tags: List<String>,
)

class SerializationTest : ThothTest() {
    private fun withMapper(block: (ObjectMapper) -> Unit) =
        thothServer { block(getKoin().get<JacksonSerialization>().objectMapper) }

    private fun ObjectMapper.read(json: String) = readValue(json, Wire::class.java)

    private fun ObjectMapper.readScalars(json: String) = readValue(json, Scalars::class.java)

    @Test
    fun `a missing key reads as absent`() =
        withMapper { mapper ->
            assertEquals(Wire(plain = null), mapper.read("""{"plain": null}"""))
        }

    @Test
    fun `a null reads as null where the field may be null`() =
        withMapper { mapper ->
            assertEquals(
                Wire(nullable = Patch.Set(null), plain = null),
                mapper.read("""{"nullable": null, "plain": null}"""),
            )
        }

    @Test
    fun `a null is rejected where the field may not be null`() =
        withMapper { mapper ->
            assertFails { mapper.read("""{"required": null, "plain": null}""") }
        }

    @Test
    fun `a value reads as set`() =
        withMapper { mapper ->
            assertEquals(
                Wire(required = Patch.Set("a"), nullable = Patch.Set("b"), plain = "c"),
                mapper.read("""{"required": "a", "nullable": "b", "plain": "c"}"""),
            )
        }

    @Test
    fun `only a patch may be left out`() =
        withMapper { mapper ->
            assertFails("a nullable key that is not a Patch must still be sent") { mapper.read("{}") }
        }

    @Test
    fun `values are not coerced into another type`() =
        withMapper { mapper ->
            val valid = """{"count": 1, "flag": true, "name": "a", "tags": ["x"]}"""
            assertEquals(Scalars(1, true, "a", listOf("x")), mapper.readScalars(valid))

            assertFails("a string is not a number") { mapper.readScalars(valid.replace("1,", "\"1\",")) }
            assertFails("a fraction is not a whole number") { mapper.readScalars(valid.replace("1,", "1.5,")) }
            assertFails("a string is not a boolean") { mapper.readScalars(valid.replace("true", "\"true\"")) }
            assertFails("a number is not a string") { mapper.readScalars(valid.replace("\"a\"", "1")) }
            assertFails("a list of non null strings holds no null") {
                mapper.readScalars(valid.replace("[\"x\"]", "[null]"))
            }
            assertFails("a single value is not a list") { mapper.readScalars(valid.replace("[\"x\"]", "\"x\"")) }
        }

    @Test
    fun `an absent patch is left out and every null is written`() =
        withMapper { mapper ->
            assertEquals("""{"plain":null}""", mapper.writeValueAsString(Wire(plain = null)))
            assertEquals(
                """{"nullable":null,"plain":null}""",
                mapper.writeValueAsString(Wire(nullable = Patch.Set(null), plain = null)),
            )
            assertEquals(
                """{"required":"a","nullable":"b","plain":"c"}""",
                mapper.writeValueAsString(Wire(Patch.Set("a"), Patch.Set("b"), "c")),
            )
        }

    @Test
    fun `every state survives a round trip`() =
        withMapper { mapper ->
            val states =
                listOf(
                    Wire(plain = null),
                    Wire(nullable = Patch.Set(null), plain = null),
                    Wire(Patch.Set("a"), Patch.Set("b"), "c"),
                    Wire(required = Patch.Set("a"), nullable = Patch.Set(null), plain = "c"),
                )
            for (state in states) {
                assertEquals(state, mapper.read(mapper.writeValueAsString(state)))
            }
        }

    @Test
    fun `a patch reads its value with the type argument`() =
        withMapper { mapper ->
            assertEquals(Tagged(Patch.Set(listOf("x"))), mapper.readValue("""{"tags": ["x"]}""", Tagged::class.java))
        }

    @Test
    fun `a metadata field goes over the wire under the name the generated clients use`() =
        withMapper { mapper ->
            assertEquals("\"BORN_IN\"", mapper.writeValueAsString(AuthorField.BORN_IN))
            assertEquals(AuthorField.BORN_IN, mapper.readValue("\"BORN_IN\"", AuthorField::class.java))
        }
}
