package plugin.loader

import kotlinx.serialization.Serializable

@Serializable
class PluginConfigFile(
    val search: List<PluginConfig>,
    val scan: List<PluginConfig>,
) {
    @Serializable
    class PluginConfig(
        val pluginClassName: String,
        val jarLocation: String,
    )
}
