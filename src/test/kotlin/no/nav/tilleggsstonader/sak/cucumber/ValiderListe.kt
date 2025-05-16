package no.nav.tilleggsstonader.sak.cucumber

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import org.assertj.core.api.Assertions
import org.slf4j.LoggerFactory

fun <T> verifiserAtListerErLike(
    faktisk: List<T>,
    forventet: List<T>,
) {
    val logger = LoggerFactory.getLogger("validerListe")
    Assertions
        .assertThat(faktisk)
        .withFailMessage { "Lengden på faktisk liste er ${faktisk.size}, mens lengden på forventet liste er ${forventet.size}" }
        .hasSize(forventet.size)

    faktisk.forEachIndexed { index, verdi ->
        val forventetVerdi = forventet[index]
        val actualPretty =
            ObjectMapperProvider.objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(verdi)
        val expectedPretty =
            ObjectMapperProvider.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(forventetVerdi)
        try {
            Assertions.assertThat(actualPretty).isEqualTo(expectedPretty)
        } catch (e: Throwable) {
            logger.error(
                "Feilet verifisering av rad ${index + 1}:\n" +
                    "--- Forventet ---\n$expectedPretty\n" +
                    "--- Faktisk -----\n$actualPretty",
            )
            throw e
        }
    }
}
