pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // One line per Minecraft version. Every target is a project named "<version>-<loader>" built by build.gradle.
        fun minecraft(version: String, vararg loaders: String) {
            for (loader in loaders) version("$version-$loader", version).buildscript("build.gradle")
        }

        // Forge 1.14.4 ships no Mixin, so only Fabric is built for it.
        minecraft("1.14.4", "fabric")
        minecraft("1.15.2", "fabric", "forge")
        minecraft("1.16.5", "fabric", "forge")
        minecraft("1.17.1", "fabric", "forge")
        minecraft("1.18.2", "fabric", "forge")
        minecraft("1.19.2", "fabric", "forge")
        minecraft("1.19.3", "fabric", "forge")
        minecraft("1.19.4", "fabric", "forge")
        minecraft("1.20.1", "fabric", "forge")
        minecraft("1.20.2", "fabric")
        minecraft("1.20.4", "fabric", "neoforge")
        minecraft("1.20.6", "fabric", "neoforge")
        minecraft("1.21.1", "fabric", "neoforge")
        minecraft("1.21.3", "fabric", "neoforge")
        minecraft("1.21.4", "fabric", "neoforge")
        minecraft("1.21.5", "fabric", "neoforge")
        minecraft("1.21.8", "fabric", "neoforge")
        minecraft("1.21.10", "fabric", "neoforge")
        minecraft("1.21.11", "fabric", "neoforge")
        minecraft("26.1", "fabric")
        minecraft("26.1.1", "fabric")
        minecraft("26.1.2", "fabric", "neoforge")
        minecraft("26.2", "fabric", "neoforge")
        minecraft("26.3", "fabric", "neoforge")
        vcsVersion = "1.21.4-fabric"
    }
}

// Which Loom builds a target. loom-back-compat puts the plugin on the classpath before any build script runs,
// so the choice is made here. The root project must stay without a Loom: its classpath is inherited by every
// target, and the two Looms share class names. NeoForge targets use ModDevGradle instead (see build.gradle).
gradle.lifecycle.beforeProject {
    val properties = extensions.extraProperties
    if (name.endsWith("-fabric")) {
        properties["loomx.loom_version"] = "1.19.4"
    } else if (name.endsWith("-forge")) {
        // Architectury Loom is the one maintained toolchain that still covers Forge 1.16.5.
        properties["loom.platform"] = "forge"
        properties["loomx.loom_version"] = "1.17.493"
        properties["loomx.loom_remap_plugin"] = "dev.architectury.loom"
        properties["loomx.loom_unobf_plugin"] = "dev.architectury.loom-no-remap"
    }
}

rootProject.name = "simplymirrorsplus"
include("core")
