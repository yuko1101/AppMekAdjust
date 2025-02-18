plugins {
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val common: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false

    configurations.getByName("compileClasspath").extendsFrom(this)
    configurations.getByName("runtimeClasspath").extendsFrom(this)
    configurations.getByName("developmentNeoForge").extendsFrom(this)
}

// Files in this configuration will be bundled into your mod using the Shadow plugin.
// Don't use the `shadow` configuration from the plugin itself as it's meant for excluding files.
val shadowBundle: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

repositories {
    maven(url = "https://maven.neoforged.net/releases")
}

dependencies {
    neoForge("net.neoforged:neoforge:${rootProject.properties["neoforge_version"]!!}")

    common(project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowBundle(project(path = ":common", configuration = "transformProductionNeoForge"))

    modImplementation("curse.maven:guideme-1173950:6179772")
    modImplementation("curse.maven:applied-energistics-2-223794:6193778")
    modImplementation("curse.maven:mekanism-268560:6018306")
    modImplementation("curse.maven:applied-mekanistics-574300:5978711")
    modImplementation("curse.maven:glodium-957920:5821676")
    modImplementation("curse.maven:applied-flux-965012:6171002")
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "ae2_version" to rootProject.properties["ae2_version"]!!,
        "mekanism_version" to rootProject.properties["mekanism_version"]!!,
        "appmek_version" to rootProject.properties["appmek_version"]!!
    )
    inputs.properties(props)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
}
