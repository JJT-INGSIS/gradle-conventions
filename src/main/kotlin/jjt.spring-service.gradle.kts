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

val sharedDetektConfig: String =
    checkNotNull(object {}.javaClass.getResource("/jjt/detekt.yml")) {
        "jjt/detekt.yml is missing from the gradle-conventions jar"
    }.readText()

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