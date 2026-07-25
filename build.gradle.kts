import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = libs.versions.kotlin.get()

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    group = "io.thoth"
    version = "0.0.1"

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
        }
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
