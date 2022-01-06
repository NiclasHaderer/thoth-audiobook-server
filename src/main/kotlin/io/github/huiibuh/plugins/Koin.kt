package io.github.huiibuh.plugins

import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.MetadataWrapper
import io.ktor.application.*
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        module {
            single<MetadataProvider> { MetadataWrapper(listOf()) }
        }
    }
}
