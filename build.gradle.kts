import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("dev.architectury.loom") version "1.9-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    java
    `maven-publish`
}

architectury {
    minecraft = project.properties["minecraft_version"] as String
}

allprojects {
    group = rootProject.properties["maven_group"]!!
    version = rootProject.properties["mod_version"]!!
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    val loom = extensions.getByName("loom") as LoomGradleExtensionAPI

    base {
        // Set up a suffixed format for the mod jar names, e.g. `example-fabric`.
        archivesName = "${rootProject.properties["archives_name"]!!}-${project.name}"
    }

    repositories {
        maven(url = "https://cursemaven.com")
    }

    dependencies {
        val minecraft = configurations.getByName("minecraft")
        val mappings = configurations.getByName("mappings")

        minecraft("net.minecraft:minecraft:${rootProject.properties["minecraft_version"]!!}")
        mappings(loom.officialMojangMappings())
    }

    java {
        // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
        // if it is present.
        // If you remove this line, sources will not be generated.
        withSourcesJar()

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
    }

    // Configure Maven publishing.
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = base.archivesName.get()
                from(components["java"])
            }
        }

        // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
        repositories {
            // Add repositories to publish to here.
            // Notice: This block does NOT have the same function as the block in the top level.
            // The repositories here will be used for publishing your artifact, not for
            // retrieving dependencies.
        }
    }
}
