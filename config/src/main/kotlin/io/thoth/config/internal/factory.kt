package io.thoth.config.internal

fun loadPrivateConfig(): ModifiablePrivateConfig =  PrivateConfigImpl.load()