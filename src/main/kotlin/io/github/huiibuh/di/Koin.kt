package io.github.huiibuh.di

import io.ktor.application.*
import org.koin.ktor.ext.Koin
import org.koin.logger.slf4jLogger

/**
 * Application is necessary to force the start of Koin in the App module
 */
fun Application.configureKoin() {
    install(Koin) {
        modules(DI_MODULE)
        slf4jLogger()
    }
}
