package plugin.loader

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader


class PluginLoader(private val pluginBasePath: String, private val pluginConfigLocation: String) {
    private val classLoader = Thread.currentThread().contextClassLoader
    private val log = LoggerFactory.getLogger(this::class.java)
    val searchPlugins: List<SearchPlugin>
    val scanPlugins: List<ScanPlugin>

    private val pluginConfigFile by lazy {
        val file = File(pluginConfigLocation)
        Json.decodeFromString<PluginConfigFile>(file.readText())
    }

    init {
        this.searchPlugins = pluginConfigFile.search
                .mapNotNull { loadPlugin("$pluginBasePath/${it.jarLocation}", it.pluginClassName) }
                .map { it.viewAs() }
        this.scanPlugins = pluginConfigFile.scan
                .mapNotNull { loadPlugin("$pluginBasePath/${it.jarLocation}", it.pluginClassName) }
                .map { it.viewAs() }
    }

    private fun loadPlugin(pluginLocation: String, className: String): Any? {
        val jarLocation = File(pluginLocation)
        val jarURL = jarLocation.toURI().toURL()
        val pluginClassLoader = URLClassLoader(arrayOf(jarURL), this.javaClass.classLoader)
        return try {
            Thread.currentThread().contextClassLoader = pluginClassLoader
            val classToLoad = Class.forName(className, true, pluginClassLoader)
            val m = classToLoad.getDeclaredConstructor().newInstance().javaClass.methods
            classToLoad.getDeclaredConstructor().newInstance()
        } catch (e: ClassNotFoundException) {
            log.error("Could not find plugin with class $className in $pluginLocation.\n Ignoring plugin.")
            null
        } finally {
            Thread.currentThread().contextClassLoader = classLoader
        }
    }
}
