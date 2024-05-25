plugins {
    `maven-publish`
    kotlin("jvm") version "1.9.22"
    id("fabric-loom") version "1.6-SNAPSHOT"
}

class ModData {
    val id = property("mod.id").toString()
    val name = property("mod.name").toString()
    val version = property("mod.version").toString()
    val group = property("mod.group").toString()
}
val kotlin = "1.9.22"
val mod = ModData()
val mcDep = property("mod.mc_dep").toString()

version = mod.version
group = mod.group
base { archivesName.set(mod.id) }

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
    fun fapiModules(vararg modules: String) {
        modules.forEach { fabricApi.module(it, "${property("deps.fapi")}") }
    }

    minecraft("com.mojang:minecraft:${mcDep}")
    mappings("net.fabricmc:yarn:${mcDep}+build.${property("deps.yarn_build")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.flk")}+kotlin.$kotlin")

//    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("carpet:fabric-carpet:${property("deps.carpet")}")
}

java {
    withSourcesJar()
}

tasks.processResources {
    inputs.property("id", mod.id)
    inputs.property("name", mod.name)
    inputs.property("version", mod.version)
    inputs.property("mcdep", mcDep)

    val map = mapOf(
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "mcdep" to mcDep
    )

    filesMatching("fabric.mod.json") { expand(map) }
}