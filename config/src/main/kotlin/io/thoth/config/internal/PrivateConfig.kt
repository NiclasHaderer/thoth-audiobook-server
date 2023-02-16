package io.thoth.config.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource
import com.sksamuel.hoplite.json.JsonParser
import io.thoth.common.utils.lazyCallable
import io.thoth.config.public.getConfigPath
import java.nio.file.Files
import java.nio.file.Path


interface ModifiableMetadataAgent  {
    val name: String
    val config: Any
}


interface ModifiableLibrary {
    val name: String
    val icon: String
    val preferEmbeddedMetadata: Boolean
    val folders: List<String>
    val metadataScanners: List<ModifiableMetadataAgent>
}


interface ModifiablePrivateConfig  {
    val libraries: List<ModifiableLibrary>
    val scanIndex: ULong
}

data class MetadataAgentImpl(
    override var name: String,
    override var config: Any
) : ModifiableMetadataAgent

data class LibraryImpl(
    override var name: String,
    override var icon: String,
    override var preferEmbeddedMetadata: Boolean,
    override var folders: List<String>,
    override var metadataScanners: List<MetadataAgentImpl>
) : ModifiableLibrary

data class PrivateConfigImpl(
    override var libraries: List<LibraryImpl>,
    override var scanIndex: ULong
) : ModifiablePrivateConfig {
    internal companion object {
        private val configPath by lazy {
            val configPath = getConfigPath()
            "$configPath/private/config.json"
        }

        private val objectMapper by lazy {
            ObjectMapper().registerKotlinModule()
        }

        internal fun saveToFile() {
            val configString = objectMapper.writeValueAsString(this)
            Files.writeString(Path.of(configPath), configString)
        }

        internal var load = lazyCallable<ModifiablePrivateConfig> {
            ConfigLoaderBuilder.empty()
                .addDefaultDecoders()
                .addParser("json", JsonParser())
                .addResourceOrFileSource(configPath)
                .build()
                .loadConfigOrThrow<PrivateConfigImpl>()
        }
    }
}

fun ModifiablePrivateConfig.modify(block: PrivateConfigImpl.() -> Unit): ModifiablePrivateConfig {
    val modifiable = this as PrivateConfigImpl
    modifiable.block()
    PrivateConfigImpl.saveToFile()
    return modifiable
}
