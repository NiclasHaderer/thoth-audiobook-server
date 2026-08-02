import groovy.json.JsonOutput
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.w3c.dom.Element
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

val generateLicenseReport =
    tasks.register("generateLicenseReport") {
        description = "Writes license metadata for every third party dependency shipped in the server jar"
        val outputDir = layout.buildDirectory.dir("generated/licenses")
        // runtimeClasspath is exactly what shadowJar packages, so test and compileOnly deps stay out of the report
        val runtimeClasspath = configurations.named("runtimeClasspath")
        val dependencyHandler = dependencies
        val configurationContainer = configurations
        inputs.files(runtimeClasspath)
        outputs.dir(outputDir)

        doLast {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

            fun childrenNamed(
                element: Element,
                name: String,
            ): List<Element> =
                (0 until element.childNodes.length)
                    .mapNotNull { element.childNodes.item(it) as? Element }
                    .filter { it.tagName == name }

            fun childNamed(
                element: Element,
                name: String,
            ): Element? = childrenNamed(element, name).firstOrNull()

            // Poms interpolate properties Maven would expand; an unexpanded placeholder is worse than nothing
            fun text(element: Element?): String? =
                element
                    ?.textContent
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.contains("\${") }

            val pomCache = mutableMapOf<String, File?>()

            fun fetchPom(coordinates: String): File? =
                pomCache.getOrPut(coordinates) {
                    runCatching {
                        configurationContainer
                            .detachedConfiguration(dependencyHandler.create("$coordinates@pom"))
                            .resolve()
                            .firstOrNull()
                    }.getOrNull()
                }

            // Licenses and scm urls are routinely declared once in a parent pom and inherited by every module
            fun pomMetadata(
                pom: File?,
                depth: Int = 0,
            ): PomMetadata {
                if (pom == null || depth > 5) return PomMetadata()
                val root = runCatching { documentBuilder.parse(pom).documentElement }.getOrNull() ?: return PomMetadata()
                val declared =
                    childNamed(root, "licenses")
                        ?.let { childrenNamed(it, "license") }
                        ?.mapNotNull { license -> text(childNamed(license, "name"))?.let { it to text(childNamed(license, "url")) } }
                        ?: emptyList()
                val own =
                    PomMetadata(
                        licenses = declared.map { (name, _) -> name },
                        licenseUrl = declared.firstNotNullOfOrNull { (_, url) -> url },
                        repository = text(childNamed(root, "scm")?.let { childNamed(it, "url") }) ?: text(childNamed(root, "url")),
                    )
                if (own.licenses.isNotEmpty() && own.repository != null) return own

                val parent = childNamed(root, "parent") ?: return own
                val coordinates = listOf("groupId", "artifactId", "version").map { text(childNamed(parent, it)) ?: return own }
                val inherited = pomMetadata(fetchPom(coordinates.joinToString(":")), depth + 1)
                return PomMetadata(
                    licenses = own.licenses.ifEmpty { inherited.licenses },
                    licenseUrl = own.licenseUrl ?: inherited.licenseUrl,
                    repository = own.repository ?: inherited.repository,
                )
            }

            val licenseFile = Regex("^(meta-inf/)?(licen[cs]e|copying)([.\\-_].*)?$", RegexOption.IGNORE_CASE)
            val noticeFile = Regex("^(meta-inf/)?notice([.\\-_].*)?$", RegexOption.IGNORE_CASE)

            fun licenseText(jar: File): String? =
                runCatching {
                    ZipFile(jar).use { zip ->
                        val entries = zip.entries().asSequence().filter { !it.isDirectory && it.size < 200_000 }.toList()

                        fun read(name: Regex): String? =
                            entries
                                .filter { name.matches(it.name) }
                                .minByOrNull { it.name.length }
                                ?.let { zip.getInputStream(it).readBytes().toString(Charsets.UTF_8).trim() }
                                ?.takeIf { it.isNotEmpty() }

                        // Apache-2.0 section 4(d) requires the NOTICE to be redistributed alongside the license
                        listOfNotNull(read(licenseFile), read(noticeFile)?.let { "NOTICE\n\n$it" })
                            .joinToString("\n\n")
                            .takeIf { it.isNotEmpty() }
                    }
                }.getOrNull()

            val artifacts =
                runtimeClasspath
                    .get()
                    .incoming
                    .artifacts
                    .artifacts
                    .mapNotNull { artifact ->
                        val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
                        id to artifact.file
                    }.distinctBy { (id, _) -> id.displayName }

            val poms =
                dependencyHandler
                    .createArtifactResolutionQuery()
                    .forComponents(artifacts.map { (id, _) -> id })
                    .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
                    .execute()
                    .resolvedComponents
                    .mapNotNull { component ->
                        val pom =
                            component
                                .getArtifacts(MavenPomArtifact::class.java)
                                .filterIsInstance<ResolvedArtifactResult>()
                                .firstOrNull()
                                ?.file ?: return@mapNotNull null
                        component.id.displayName to pom
                    }.toMap()

            val packages =
                artifacts
                    .map { (id, jar) ->
                        val pom = pomMetadata(poms[id.displayName])
                        linkedMapOf(
                            "name" to "${id.group}:${id.module}",
                            "version" to id.version,
                            "license" to pom.licenses.joinToString(", ").ifEmpty { "UNKNOWN" },
                            "licenseUrl" to pom.licenseUrl,
                            "repository" to pom.repository,
                            "text" to licenseText(jar),
                        )
                    }.sortedBy { it["name"] }

            val target = outputDir.get().asFile
            target.mkdirs()
            target
                .resolve("third-party-licenses.json")
                .writeText(JsonOutput.prettyPrint(JsonOutput.toJson(packages as Any)) + "\n")
            logger.lifecycle("Wrote ${packages.size} licenses to ${target.resolve("third-party-licenses.json")}")
        }
    }

the<SourceSetContainer>()["main"].resources.srcDir(generateLicenseReport)
