import dev.detekt.gradle.Detekt
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")

    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")

    jacoco
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

ktlint {
    version.set("1.8.0")
    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "ktlint_official",
            "max_line_length" to "120",
        ),
    )
}

fun sharedResource(path: String): String =
    checkNotNull(object {}.javaClass.getResource(path)) {
        "$path is missing from the gradle-conventions jar"
    }.readText()

val sharedDetektConfig: String = sharedResource("/jjt/detekt.yml")

detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(resources.text.fromString(sharedDetektConfig).asFile())
}

tasks.named("check") {
    setDependsOn(dependsOn.filterNot { it is TaskProvider<*> && it.name == "detekt" })
    dependsOn(tasks.named<Detekt>("detektMain"), tasks.named<Detekt>("detektTest"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))

    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestReport"))
}

val gitHookScripts: Map<String, String> =
    listOf("pre-commit", "pre-push").associateWith { hook -> sharedResource("/jjt/git-hooks/$hook") }

val gitHooksDirectory: Provider<String> =
    providers
        .exec {
            workingDir(layout.projectDirectory)
            commandLine("git", "rev-parse", "--path-format=absolute", "--git-path", "hooks")
        }.standardOutput.asText
        .map { it.trim() }

tasks.register("installGitHooks") {
    group = "git hooks"
    description = "Installs the shared pre-commit and pre-push hooks in this clone."

    val hooksDirectory = gitHooksDirectory
    val scripts = gitHookScripts

    doLast {
        val directory = File(hooksDirectory.get())
        directory.mkdirs()
        scripts.forEach { (hook, script) ->
            File(directory, hook).apply {
                writeText(script)
                setExecutable(true)
            }
        }
        logger.lifecycle("Installed ${scripts.keys.joinToString()} in $directory")
    }
}
