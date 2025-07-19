plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("fabric-loom") version "1.11-SNAPSHOT"
}

version = property("mod.version") as String
group = property("mod.group") as String
base.archivesName = property("mod.id") as String
project.
loom {
    splitEnvironmentSourceSets()

    mods {
        create("template") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

repositories {
    exclusiveContent {
        forRepository { maven("https://www.cursemaven.com") { name = "CurseForge" } }
        filter { includeGroup("curse.maven") }
    }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://masa.dy.fi/maven") { name = "Masa Maven" }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("mod.mc_dep")}")
    mappings("net.fabricmc:yarn:${property("mod.mc_dep")}+build.${property("deps.yarn_build")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.flk")}")

    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("carpet:fabric-carpet:${property("deps.carpet")}")
}

java {
    withSourcesJar()
}

tasks.processResources {
    inputs.property("id", project.property("mod.id"))
    inputs.property("name", project.property("mod.name"))
    inputs.property("version", project.property("mod.version"))
    inputs.property("mcdep", project.property("mod.mc_dep"))

    val map = mapOf(
        "id" to project.property("mod.id"),
        "name" to project.property("mod.name"),
        "version" to project.property("mod.version"),
        "mcdep" to project.property("mod.mc_dep")
    )

    filesMatching("fabric.mod.json") { expand(map) }
}