package io.thoth.openapi

import io.ktor.server.testing.*
import io.thoth.openapi.client.typescript.generateTsClient
import kotlin.test.Test
import kotlin.test.assertEquals

class TsGenerationTest {

    @Test
    fun testSomething() {
        val generatedStuff = mutableMapOf<String, String>()
        testApplication {
            application {
                testRoutes()
                generateTsClient(
                    dist = "",
                    fileWriter = { file, content -> generatedStuff[file.name] = content },
                )
            }
        }
        val models = """
        import type { Pair } from "./utility-types";
        export type UUID = `$\{string}-$\{string}-$\{string}-$\{string}-$\{string}`
        
        export interface ListRoute  {
          name: string;
          someParam: Array<UUID>;
        }
        
        export interface MapRoute  {
          name: boolean;
          someParam: Record<string, UUID>;
        }
        
        export interface SetRoute  {
          name: number;
          someParam: Array<UUID>;
        }
        
        export interface GenericRoute<T>  {
          name: Pair<any, UUID>;
          someParam: T;
        }
        
        export interface GenericRoute2<T , U>  {
          name: string;
          someParam: T;
          someParam2: U;
        }
        
        export interface GenericRoute3<T>  {
          name: string;
          someParam: Record<string, T>;
          someParam2: GenericRoute2<T, string>;
        }
        """.trimIndent().replace("\\{", "{")

        val generatedModels = generatedStuff["models.ts"]
        assertEquals(models, generatedModels)
    }
}
