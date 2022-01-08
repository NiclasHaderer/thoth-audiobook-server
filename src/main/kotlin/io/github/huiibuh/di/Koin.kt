package io.github.huiibuh.di

import org.koin.core.context.GlobalContext.startKoin

fun configureKoin() {
    startKoin {
        modules(DI_MODULE)
    }
}
