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
                    apiFactoryName = "createTestApi"
                )
            }
        }
        val models =
            """
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
        """
                .trimIndent()
                .replace("\\{", "{")

        val generatedModels = generatedStuff["models.ts"]
        assertEquals(models, generatedModels)

        val apiClient =
            """
        // noinspection JSUnusedGlobalSymbols,ES6UnusedImports
        import {ApiCallData, ApiInterceptor, ApiResponse, _request, _createUrl, _mergeHeaders} from "./client";
        import type {GenericRoute, GenericRoute2, GenericRoute3, ListRoute, MapRoute, SetRoute, UUID} from "./models";
        
        export const createTestApi = (
          defaultHeaders: HeadersInit = {},
          defaultInterceptors: ApiInterceptor[] = [],
          executor = (callData: ApiCallData) => fetch(callData.route, {method: callData.method, headers: callData.headers, body: callData.bodySerializer(callData.body)})
        ) => {
          const defaultHeadersImpl = new Headers(defaultHeaders)
          return {
            v1({name, someParam, path}: {name: string,someParam: Array<number>,path: UUID}, headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []): Promise<ApiResponse<ListRoute>> {
              return _request(_createUrl(`/$\{path}/V1`, {name, someParam}), "GET", "json", _mergeHeaders(defaultHeadersImpl, headers), undefined, [...defaultInterceptors, ...interceptors], executor, false);
            },
            v2({name, someParam, path}: {name: string,someParam: Array<number>,path: UUID}, headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []): Promise<ApiResponse<MapRoute>> {
              return _request(_createUrl(`/$\{path}/V2`, {name, someParam}), "GET", "json", _mergeHeaders(defaultHeadersImpl, headers), undefined, [...defaultInterceptors, ...interceptors], executor, false);
            },
            v3({name, someParam, path}: {name: string,someParam: Array<number>,path: UUID}, headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []): Promise<ApiResponse<SetRoute>> {
              return _request(_createUrl(`/$\{path}/V3`, {name, someParam}), "GET", "json", _mergeHeaders(defaultHeadersImpl, headers), undefined, [...defaultInterceptors, ...interceptors], executor, false);
            },
            v4({name, someParam, path}: {name: string,someParam: Array<number>,path: UUID}, headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []): Promise<ApiResponse<GenericRoute<UUID>>> {
              return _request(_createUrl(`/$\{path}/V4`, {name, someParam}), "GET", "json", _mergeHeaders(defaultHeadersImpl, headers), undefined, [...defaultInterceptors, ...interceptors], executor, false);
            },
            v5({name, someParam, path}: {name: string,someParam: Array<number>,path: UUID}, body: GenericRoute2<string, UUID>, headers: HeadersInit = {}, interceptors: ApiInterceptor[] = []): Promise<ApiResponse<GenericRoute3<string>>> {
              return _request(_createUrl(`/$\{path}/V5`, {name, someParam}), "POST", "json", _mergeHeaders(defaultHeadersImpl, headers), body, [...defaultInterceptors, ...interceptors], executor, false);
            }
          } as const;
        }
        """
                .trimIndent()
                .replace("\\{", "{")

        val generatedApiClient = generatedStuff["api-client.ts"]
        assertEquals(apiClient, generatedApiClient)
    }
}
