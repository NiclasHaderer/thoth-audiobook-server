import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

val kotlinVersion = libs.versions.kotlin.get()
val ktlintVersion = libs.versions.ktlint.get()

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint)
}

val gitDir = layout.projectDirectory.file(".git").asFile
val installGitHooks =
    tasks.register<Copy>("installGitHooks") {
        onlyIf { gitDir.isDirectory }
        from(layout.projectDirectory.dir(".githooks"))
        into(gitDir.resolve("hooks"))
        filePermissions { unix("rwxr-xr-x") }
    }

subprojects {
    group = "io.thoth"
    version = "0.0.1"

    tasks.matching { it.name == "compileKotlin" }.configureEach { dependsOn(installGitHooks) }

    apply<KtlintPlugin>()
    configure<KtlintExtension> {
        version.set(ktlintVersion)
        filter { exclude { it.file.path.contains("${File.separator}gen${File.separator}") } }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
        }
    }
    // Koin, Exposed's TransactionManager and the pipeline threads are all JVM-global, so classes may only
    // run side by side in separate JVMs
    tasks.withType<Test>().configureEach {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            freeCompilerArgs.add("-Xcontext-parameters")
            freeCompilerArgs.add("-Xmulti-dollar-interpolation")
            jvmTarget.set(JvmTarget.JVM_25)
            apiVersion.set(KotlinVersion.fromVersion(kotlinVersion.substringBeforeLast('.')))
            languageVersion.set(KotlinVersion.fromVersion(kotlinVersion.substringBeforeLast('.')))
            optIn.add("kotlin.RequiresOptIn")
        }
    }
}
