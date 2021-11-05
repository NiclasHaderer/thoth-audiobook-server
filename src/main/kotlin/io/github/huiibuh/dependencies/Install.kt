package io.github.huiibuh.dependencies


import io.ktor.application.*
import org.koin.ktor.ext.Koin
import org.koin.logger.SLF4JLogger


fun Application.configureKoin() {
    install(Koin) {
        SLF4JLogger()
        modules(AudibleAppModule)
    }
}
