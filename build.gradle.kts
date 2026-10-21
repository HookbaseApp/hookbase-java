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
    signAllPublications()
    coordinates(project.group.toString(), "hookbase-java", project.version.toString())
}
