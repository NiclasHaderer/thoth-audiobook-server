package io.thoth.common.extensions

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import org.koin.ktor.ext.inject

inline fun <reified T : Any> NormalOpenAPIRoute.inject(
): Lazy<T> {
    return this.ktorRoute.inject()
}

