private object ConventionResources

fun conventionResource(path: String): String =
    checkNotNull(ConventionResources::class.java.getResource(path)) {
        "$path is missing from the gradle-conventions jar"
    }.readText()
