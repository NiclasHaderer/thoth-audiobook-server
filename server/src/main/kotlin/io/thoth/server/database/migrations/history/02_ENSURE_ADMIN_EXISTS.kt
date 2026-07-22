package io.thoth.server.database.migrations.history

import io.thoth.server.database.migrations.Migration
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// Enforces "at least one admin" at the DB level. Only SQLite and PostgreSQL are supported (see DatabaseConnector),
// and their trigger dialects differ, so this branches per dialect. SQLite is race-free because it serializes
// writers; the Postgres function locks all admin rows with FOR UPDATE to serialize concurrent demotions/deletes.
class `02_ENSURE_ADMIN_EXISTS` : Migration() {
    override fun migrate() {
        transaction {
            when (val dialect = currentDialect) {
                is SQLiteDialect -> {
                    exec(
                        """
                        CREATE TRIGGER IF NOT EXISTS ensure_admin_on_update
                        BEFORE UPDATE OF "admin" ON "Users"
                        FOR EACH ROW
                        WHEN OLD."admin" = 1 AND NEW."admin" = 0
                        BEGIN
                            SELECT CASE WHEN (SELECT COUNT(*) FROM "Users" WHERE "admin" = 1) <= 1
                                THEN RAISE(ABORT, 'Cannot remove the only admin user.') END;
                        END;
                        """.trimIndent(),
                    )
                    exec(
                        """
                        CREATE TRIGGER IF NOT EXISTS ensure_admin_on_delete
                        BEFORE DELETE ON "Users"
                        FOR EACH ROW
                        WHEN OLD."admin" = 1
                        BEGIN
                            SELECT CASE WHEN (SELECT COUNT(*) FROM "Users" WHERE "admin" = 1) <= 1
                                THEN RAISE(ABORT, 'Cannot delete the only admin user.') END;
                        END;
                        """.trimIndent(),
                    )
                }

                is PostgreSQLDialect -> {
                    val dollar = "${'$'}${'$'}"
                    exec(
                        """
                        CREATE OR REPLACE FUNCTION ensure_admin_exists() RETURNS trigger AS $dollar
                        BEGIN
                            IF (TG_OP = 'DELETE' AND OLD."admin")
                                OR (TG_OP = 'UPDATE' AND OLD."admin" AND NOT NEW."admin") THEN
                                PERFORM 1 FROM "Users" WHERE "admin" FOR UPDATE;
                                IF (SELECT COUNT(*) FROM "Users" WHERE "admin") <= 1 THEN
                                    RAISE EXCEPTION 'Cannot remove the only admin user.';
                                END IF;
                            END IF;
                            IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
                            RETURN NEW;
                        END;
                        $dollar LANGUAGE plpgsql;
                        """.trimIndent(),
                    )
                    exec("""DROP TRIGGER IF EXISTS ensure_admin_on_update ON "Users";""")
                    exec(
                        """
                        CREATE TRIGGER ensure_admin_on_update
                        BEFORE UPDATE OF "admin" ON "Users"
                        FOR EACH ROW EXECUTE PROCEDURE ensure_admin_exists();
                        """.trimIndent(),
                    )
                    exec("""DROP TRIGGER IF EXISTS ensure_admin_on_delete ON "Users";""")
                    exec(
                        """
                        CREATE TRIGGER ensure_admin_on_delete
                        BEFORE DELETE ON "Users"
                        FOR EACH ROW EXECUTE PROCEDURE ensure_admin_exists();
                        """.trimIndent(),
                    )
                }

                else -> error("Unsupported dialect '${dialect.name}' for the admin-existence triggers")
            }
        }
    }
}
