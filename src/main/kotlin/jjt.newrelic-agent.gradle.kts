import java.security.MessageDigest

val agentVersion = "9.5.0"
val agentSha256 = "411587286dbb83b06fcdd042ad1c9c1818655831366717aef3611962ea2c1b73"
val serviceNameToken = "@SERVICE_NAME@"
val serviceNamePattern = Regex("[a-z][a-z0-9-]{1,62}")

val newRelicAgent: Configuration by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    newRelicAgent("com.newrelic.agent.java:newrelic-agent:$agentVersion")
}

val serviceName: String = project.name
check(serviceNamePattern.matches(serviceName)) { "Unexpected service name for New Relic: $serviceName" }

val prepareNewRelicAgent by tasks.registering {
    group = "observability"
    description = "Copies the pinned New Relic agent, its configuration and the start script to build/newrelic."

    val agentFiles: FileCollection = newRelicAgent
    val outputDirectory = layout.buildDirectory.dir("newrelic")
    val configuration = conventionResource("/jjt/newrelic/newrelic.yml")
    val startScript = conventionResource("/jjt/newrelic/start-service.sh").replace(serviceNameToken, serviceName)
    val expectedSha256 = agentSha256

    inputs.files(agentFiles)
    inputs.property("agentSha256", expectedSha256)
    inputs.property("configuration", configuration)
    inputs.property("startScript", startScript)
    outputs.dir(outputDirectory)

    doLast {
        val agent = agentFiles.singleFile
        val actualSha256 =
            MessageDigest.getInstance("SHA-256").digest(agent.readBytes()).joinToString("") { "%02x".format(it) }
        check(actualSha256 == expectedSha256) { "New Relic agent checksum mismatch: $actualSha256" }

        val directory = outputDirectory.get().asFile
        directory.deleteRecursively()
        directory.mkdirs()
        agent.copyTo(directory.resolve("newrelic.jar"))
        directory.resolve("newrelic.yml").writeText(configuration)
        directory.resolve("start-service.sh").apply {
            writeText(startScript)
            setExecutable(true, false)
        }
    }
}

tasks.matching { it.name == "bootJar" }.configureEach {
    dependsOn(prepareNewRelicAgent)
}
