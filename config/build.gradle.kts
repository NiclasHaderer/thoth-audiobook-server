val hopliteVersion: String by project
val ktorVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":metadata"))
    implementation(project(":common"))

    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-json:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-toml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
}
