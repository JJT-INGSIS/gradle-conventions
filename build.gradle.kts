plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.jjt.ingsis"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.4.10")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.1.1")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:14.2.0")
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.6")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jjt-ingsis/gradle-conventions")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
