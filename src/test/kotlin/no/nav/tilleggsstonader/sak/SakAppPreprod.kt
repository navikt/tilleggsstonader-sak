package no.nav.tilleggsstonader.sak

import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.File

fun main(args: Array<String>) {
    val properties = hentPreprodEnv()
    SpringApplicationBuilder(App::class.java)
        .profiles(
            "preprod",
        ).properties(properties)
        .run(*args)
}

private fun hentPreprodEnv(): Map<String, String> {
    val cmd = "src/test/resources/hentEnvFraPreprod.sh"

    val file =
        File(
            IntegrationTest::class.java.classLoader
                .getResource("hentEnvFraPreprod.sh")!!
                .file,
        )

    val process = ProcessBuilder(file.path).start()

    if (process.waitFor() == 1) {
        error("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
    }

    return process.inputStream
        .bufferedReader()
        .use { it.readText() }
        .split("Envs:\n")[1]
        .split("\n")
        .filterNot { it.isNullOrEmpty() }
        .map { it.split("=") }
        .associate { it[0] to it[1] }
}
