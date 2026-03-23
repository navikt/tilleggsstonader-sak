package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class StegTypeTest {
    @Test
    fun `rekkefølgen på stegene skal være økende med 1 mellom hvert steg`() {
        StegType.entries.windowed(2).forEach {
            assertThat(it[0].rekkefølge + 1)
                .`as` {
                    "${it[0]} med rekkefølge ${it[0].rekkefølge} " +
                        "testes mot ${it[1]} med rekkefølge=${it[1].rekkefølge}"
                }.isEqualTo(it[1].rekkefølge)
        }
    }

    @EnumSource(
        value = StegType::class,
    )
    @ParameterizedTest
    fun `skal finne neste steg for alle relevante steg i standardflyten`(steg: StegType) {
        assertDoesNotThrow { steg.hentNesteSteg(Stønadstype.BARNETILSYN) }
        assertDoesNotThrow { steg.hentNesteSteg(Stønadstype.LÆREMIDLER) }
        assertDoesNotThrow { steg.hentNesteSteg(Stønadstype.BOUTGIFTER) }
        assertDoesNotThrow { steg.hentNesteSteg(Stønadstype.DAGLIG_REISE_TSO) }
        assertDoesNotThrow { steg.hentNesteSteg(Stønadstype.DAGLIG_REISE_TSR) }
    }

    @EnumSource(
        value = StegType::class,
        names = ["KJØRELISTE", "BEREGNING", "SIMULERING", "FULLFØR_KJØRELISTE", "FERDIGSTILLE_BEHANDLING", "BEHANDLING_FERDIGSTILT"],
        mode = EnumSource.Mode.INCLUDE,
    )
    @ParameterizedTest
    fun `skal finne neste for relevante steg for en kjørelistebehandling`(steg: StegType) {
        assertDoesNotThrow { steg.hentNesteStegKjørelistebehandling() }
    }
}
