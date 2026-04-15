package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BeregningsresultatLæremidlerDtoTest {
    @Test
    fun `skal mappe til dto`() {
        val dto =
            BeregningsresultatLæremidler(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            1 januar 2024,
                            31 januar 2024,
                        ),
                        beregningsresultatForMåned(
                            1 februar 2024,
                            29 februar 2024,
                            utbetalingsdato = 1 januar 2024,
                        ),
                        beregningsresultatForMåned(
                            1 mai 2024,
                            31 mai 2024,
                        ),
                    ),
            ).tilDto(beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER))

        assertThat(dto.perioder).containsExactlyInAnyOrder(
            beregningsresultatForPeriodeDto(
                fom = 1 januar 2024,
                tom = 29 februar 2024,
                antallMåneder = 2,
                stønadsbeløpForPeriode = 1750,
            ),
            beregningsresultatForPeriodeDto(
                fom = 1 mai 2024,
                tom = 31 mai 2024,
            ),
        )
    }

    @Test
    fun `skal ikke filtrere perioder ved gjenbruk av forrige resultat`() {
        val dto =
            BeregningsresultatLæremidler(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            1 januar 2024,
                            31 januar 2024,
                        ),
                        beregningsresultatForMåned(
                            1 februar 2024,
                            29 februar 2024,
                            utbetalingsdato = 1 januar 2024,
                        ),
                    ),
            ).tilDto(beregningsplan = Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT))

        assertThat(dto.perioder).containsExactly(
            beregningsresultatForPeriodeDto(
                fom = 1 januar 2024,
                tom = 29 februar 2024,
                antallMåneder = 2,
                stønadsbeløpForPeriode = 1750,
            ),
        )
        assertThat(dto.tidligsteEndring).isNull()
    }
}
