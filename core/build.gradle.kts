plugins { `java-library` }

repositories { mavenCentral() }

dependencies {
    // The oldest LWJGL 3 any supported Minecraft ships (1.14), so newer calls cannot slip in.
    compileOnly("org.lwjgl:lwjgl:3.2.2")
    compileOnly("org.lwjgl:lwjgl-opengl:3.2.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}
