package io.thoth.server.common.extensions

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The rendered SQL is asserted rather than executed, since what is being covered is how it is built. */
class ILikeOpTest {
    private object Sample : Table("sample") {
        val name = varchar("name", 255)
    }

    // LikePattern.ofLiteral reads the dialect's wildcard set, so even building a pattern needs a transaction.
    private fun renderOnSqlite(build: () -> Op<Boolean>): String {
        val db =
            Database.connect(
                "jdbc:sqlite:file:ilike-op-test?mode=memory&cache=shared",
                "org.sqlite.JDBC",
                databaseConfig = DatabaseConfig {},
            )
        return transaction(db) { build().toString() }
    }

    @Test
    fun `sqlite folds both operands with LOWER`() {
        val sql = renderOnSqlite { Sample.name ilike LikePattern("%foo%") }
        assertFalse(sql.contains("ILIKE"), "sqlite has no ILIKE: $sql")
        assertEquals(
            2,
            Regex("LOWER", RegexOption.IGNORE_CASE).findAll(sql).count(),
            "column and pattern must both be folded, or the comparison is asymmetric: $sql",
        )
    }

    @Test
    fun `escape neutralises wildcards in the value`() {
        val sql = renderOnSqlite { Sample.name ilike escape("100%_x") }
        assertTrue(sql.contains("'100\\%\\_x'"), sql)
    }

    @Test
    fun `an escaped value keeps working when interpolated into a pattern`() {
        val sql = renderOnSqlite { Sample.name ilike "%${escape("100%")}%" }
        assertTrue(sql.contains("'%100\\%%'"), "the value's own '%' must be escaped, the wrappers must not: $sql")
    }

    @Test
    fun `the escape clause is always declared so interpolated escapes are honoured`() {
        assertTrue(renderOnSqlite { Sample.name ilike "%foo%" }.contains("ESCAPE"))
    }

    @Test
    fun `a plain string pattern keeps its wildcards active`() {
        val sql = renderOnSqlite { Sample.name ilike "%foo%" }
        assertTrue(sql.contains("'%foo%'"), "unescaped wildcards must reach the pattern verbatim: $sql")
    }
}
