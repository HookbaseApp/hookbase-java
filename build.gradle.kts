plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

repositories { mavenCentral() }

dependencies {
    api(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial", "-Werror"))
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        encoding = "UTF-8"
    }
    exclude("app/hookbase/internal/**")
}

// The SDK reads its own version (for the User-Agent) from this file, so the version lives only in
// gradle.properties and cannot drift the way node-sdk's SDK_VERSION constant did.
tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "app.hookbase",
            "Implementation-Title" to "hookbase-java",
            "Implementation-Version" to project.version.toString(),
        )
    }
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("app/hookbase/version.properties") { expand("version" to version) }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("hookbase.expectedVersion", project.version.toString())
}

// The release workflow passes the tag; refuse to publish a version the tag doesn't name.
val checkReleaseTag by tasks.registering {
    val tag = providers.environmentVariable("RELEASE_TAG").orElse("")
    val version = project.version.toString()
    doLast {
        val t = tag.get().removePrefix("v")
        check(t.isEmpty() || t == version) { "Release tag v$t does not match gradle.properties version $version" }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // Central rejects unsigned artifacts, so a release without the key fails there; skipping signing
    // when it's absent keeps publishToMavenLocal usable for local testing.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(project.group.toString(), "hookbase-java", project.version.toString())
}

// Prints the runtime classpath (the jar plus Jackson), for running examples/ with `java -cp`.
val printRuntimeClasspath by tasks.registering {
    val cp = tasks.jar.flatMap { it.archiveFile }.map { it.asFile.path }
    val deps = configurations.runtimeClasspath.map { c -> c.files.map { it.path } }
    dependsOn(tasks.jar)
    doLast { println((listOf(cp.get()) + deps.get()).joinToString(File.pathSeparator)) }
}

// examples/: compiled by `build` so they can't drift from the API; run one with
// `./gradlew runExample -Pexample=BasicUsage`. Not published.
val examples by sourceSets.creating {
    java.srcDir("examples/src/main/java")
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

tasks.named("check") { dependsOn(tasks.named(examples.classesTaskName)) }

tasks.register<JavaExec>("runExample") {
    description = "Runs examples/<example>.java (-Pexample=BasicUsage)."
    classpath = examples.runtimeClasspath
    mainClass = providers.gradleProperty("example").map { "examples.$it" }.orElse("examples.BasicUsage")
}
