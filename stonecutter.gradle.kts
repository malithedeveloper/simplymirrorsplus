stonecutter active "1.21.4-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)
    properties { tags(version, loader) }
    constants { match(loader, "fabric", "forge", "neoforge") }
    // Versions Chisels & Bits exists for; the link to it is left out of the others. build.gradle has the same list for the data files.
    constants["chisels"] = version in listOf("1.16.5", "1.17.1", "1.18.2", "1.19.2", "1.19.3", "1.19.4", "1.20.1", "1.20.4", "1.21.1", "1.21.10", "1.21.11", "26.1.2")
    files.disable(version !in listOf("1.16.5", "1.17.1", "1.18.2", "1.19.2", "1.19.3", "1.19.4", "1.20.1", "1.20.4", "1.21.1", "1.21.10", "1.21.11", "26.1.2"), "com/malidev/simplymirrorsplus/client/ChiselledMirrors.java")
    // Each loader's entry points live in a package of their own and are left out of the other builds.
    for (other in listOf("fabric", "forge", "neoforge")) files.disable(loader != other, "com/malidev/simplymirrorsplus/$other/")
    replacements.string(current.parsed >= "1.21.11") { replace("ResourceLocation", "Identifier") }
    // Minecraft 26.3 moved the graphics API out of blaze3d into its renderpearl library.
    replacements.string(current.parsed >= "26.3") {
        replace("com.mojang.blaze3d.buffers", "com.mojang.renderpearl.api.buffers")
        replace("com/mojang/blaze3d/buffers", "com/mojang/renderpearl/api/buffers")
        replace("com.mojang.blaze3d.textures", "com.mojang.renderpearl.api.textures")
        replace("com.mojang.blaze3d.opengl.GlTexture", "com.mojang.renderpearl.backend.opengl.GlTexture")
        replace("com.mojang.blaze3d.GpuFormat", "com.mojang.renderpearl.api.GpuFormat")
    }
}

// One folder with every release jar, ready to upload.
tasks.register<Copy>("collectJars") {
    val version = providers.gradleProperty("mod_version").get()
    for (node in subprojects.filter { it.name != "core" }) {
        dependsOn("${node.path}:build")
        from(node.layout.buildDirectory.dir("libs")) { include("simplymirrorsplus-*-$version.jar") }
    }
    into(layout.buildDirectory.dir("release"))
}
