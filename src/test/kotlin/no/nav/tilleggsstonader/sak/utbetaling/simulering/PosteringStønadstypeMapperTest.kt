package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.UtbetalingFagområde
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Periode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Postering
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.PosteringType
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class PosteringStønadstypeMapperTest {
    @Test
    fun `alle TypeAndel bortsett fra UGYLDIG skal ha en definert klassekode`() {
        val typeAndelerMedKlassekode = PosteringStønadstypeMapper.klassekodeTilTypeAndel.values.toSet()
        val forventedeTypeAndeler = TypeAndel.entries.filterNot { it == TypeAndel.UGYLDIG }

        assertThat(typeAndelerMedKlassekode).containsExactlyInAnyOrderElementsOf(forventedeTypeAndeler)
    }

    @Test
    fun `skal mappe klassekoder for ulike stønadstyper til riktig stønadstype`() {
        val perioder =
            listOf(
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSTBASISP4-OP", 10), // TILSYN_BARN_AAP
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 20), // LÆREMIDLER_AAP
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSBUASIA-OP", 30), // BOUTGIFTER_AAP
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSDRASISP1-OP", 40), // DAGLIG_REISE_AAP -> TSO
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSDRAFT-OP", 50), // DAGLIG_REISE_TILTAK_* -> TSR
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSROSASISP3-OP", 60), // REISE_TIL_SAMLING_AAP -> TSO
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSROSAFT-OP", 70), // REISE_TIL_SAMLING_TILTAK_* -> TSR
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSROAAISP3-OP", 80), // REISE_OPPSTART_AAP -> TSO
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSROAAFT-OP", 90), // REISE_OPPSTART_TILTAK_* -> TSR
            )

        val resultat =
            PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(
                perioder = perioder,
                // Velger en stønadstype som ikke finnes blant posteringene, for å få med alle i resultatet
                egenStønadstype = Stønadstype.FLYTTING_TSO,
            )

        assertThat(resultat[YearMonth.of(2024, 1)])
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    Stønadstype.BARNETILSYN to 10,
                    Stønadstype.LÆREMIDLER to 20,
                    Stønadstype.BOUTGIFTER to 30,
                    Stønadstype.DAGLIG_REISE_TSO to 40,
                    Stønadstype.DAGLIG_REISE_TSR to 50,
                    Stønadstype.REISE_TIL_SAMLING_TSO to 60,
                    Stønadstype.REISE_TIL_SAMLING_TSR to 70,
                    Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO to 80,
                    Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR to 90,
                ),
            )
    }

    @Test
    fun `skal ekskludere egen stønadstype`() {
        val perioder =
            listOf(
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSTBASISP4-OP", 10), // TILSYN_BARN_AAP -> BARNETILSYN
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 20), // LÆREMIDLER_AAP -> LÆREMIDLER
            )

        val resultat =
            PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(
                perioder = perioder,
                egenStønadstype = Stønadstype.BARNETILSYN,
            )

        assertThat(resultat[YearMonth.of(2024, 1)])
            .containsExactlyEntriesOf(mapOf(Stønadstype.LÆREMIDLER to 20))
    }

    @Test
    fun `skal ignorere ukjente klassekoder`() {
        val perioder =
            listOf(
                periodeMedPostering(LocalDate.of(2024, 1, 1), "UKJENT_KLASSEKODE", 1000),
            )

        val resultat =
            PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(
                perioder = perioder,
                egenStønadstype = Stønadstype.BARNETILSYN,
            )

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `skal summere beløp for samme stønadstype fra flere perioder i samme måned`() {
        val perioder =
            listOf(
                periodeMedPostering(LocalDate.of(2024, 1, 2), "TSLMASISP3-OP", 20),
                periodeMedPostering(LocalDate.of(2024, 1, 25), "TSLMASISP3-OP", 5),
            )

        val resultat =
            PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(
                perioder = perioder,
                egenStønadstype = Stønadstype.BARNETILSYN,
            )

        assertThat(resultat[YearMonth.of(2024, 1)])
            .containsExactlyEntriesOf(mapOf(Stønadstype.LÆREMIDLER to 25))
    }

    @Test
    fun `skal ignorere posteringer som ikke er av type YTELSE selv om klassekoden er gyldig`() {
        val perioder =
            listOf(
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 20, PosteringType.YTELSE),
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 1000, PosteringType.FEILUTBETALING),
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 500, PosteringType.MOTPOSTERING),
            )

        val resultat =
            PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(
                perioder = perioder,
                egenStønadstype = Stønadstype.BARNETILSYN,
            )

        assertThat(resultat[YearMonth.of(2024, 1)])
            .containsExactlyEntriesOf(mapOf(Stønadstype.LÆREMIDLER to 20))
    }

    @Test
    fun `skal filtrere bort stønadstyper hvor beløpet summeres til 0, som ved opphør`() {
        val perioder =
            listOf(
                // Opphør vises som en postering og en tilsvarende negativ motpostering
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 500),
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", -500),
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSBUASIA-OP", 100),
            )

        val resultat =
            PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(
                perioder = perioder,
                egenStønadstype = Stønadstype.BARNETILSYN,
            )

        assertThat(resultat[YearMonth.of(2024, 1)])
            .containsExactlyEntriesOf(mapOf(Stønadstype.BOUTGIFTER to 100))
    }

    @Test
    fun `skal ikke ha med måned i resultatet hvis alle stønadstyper summeres til 0`() {
        val perioder =
            listOf(
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 500),
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", -500),
            )

        val resultat =
            PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(
                perioder = perioder,
                egenStønadstype = Stønadstype.BARNETILSYN,
            )

        assertThat(resultat).doesNotContainKey(YearMonth.of(2024, 1))
    }

    @Test
    fun `skal gruppere beløp fra ukjente klassekoder per fagområde`() {
        val perioder =
            listOf(
                periodeMedPostering(
                    LocalDate.of(2024, 1, 1),
                    "TPTPATT-OP",
                    500,
                    fagområde = UtbetalingFagområde.TILTAKSPENGER,
                ),
                periodeMedPostering(LocalDate.of(2024, 1, 1), "TSLMASISP3-OP", 20),
            )

        val resultat = PosteringStønadstypeMapper.beløpFraUkjentKildePerMåned(perioder)

        assertThat(resultat[YearMonth.of(2024, 1)])
            .containsExactlyEntriesOf(mapOf(UtbetalingFagområde.TILTAKSPENGER to 500))
    }

    @Test
    fun `skal summere ukjente klassekoder for samme fagområde i samme måned`() {
        val perioder =
            listOf(
                periodeMedPostering(
                    LocalDate.of(2024, 1, 2),
                    "TPTPATT-OP",
                    500,
                    fagområde = UtbetalingFagområde.TILTAKSPENGER,
                ),
                periodeMedPostering(
                    LocalDate.of(2024, 1, 20),
                    "TPTPATT-OP",
                    100,
                    fagområde = UtbetalingFagområde.TILTAKSPENGER,
                ),
            )

        val resultat = PosteringStønadstypeMapper.beløpFraUkjentKildePerMåned(perioder)

        assertThat(resultat[YearMonth.of(2024, 1)])
            .containsExactlyEntriesOf(mapOf(UtbetalingFagområde.TILTAKSPENGER to 600))
    }

    @Test
    fun `skal filtrere bort ukjente klassekoder som ikke er av type YTELSE`() {
        val perioder =
            listOf(
                periodeMedPostering(
                    LocalDate.of(2024, 1, 1),
                    "TPTPATT-OP",
                    500,
                    type = PosteringType.MOTPOSTERING,
                    fagområde = UtbetalingFagområde.TILTAKSPENGER,
                ),
            )

        val resultat = PosteringStønadstypeMapper.beløpFraUkjentKildePerMåned(perioder)

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `skal filtrere bort ukjente klassekoder hvor beløpet summeres til 0`() {
        val perioder =
            listOf(
                periodeMedPostering(
                    LocalDate.of(2024, 1, 1),
                    "TPTPATT-OP",
                    500,
                    fagområde = UtbetalingFagområde.TILTAKSPENGER,
                ),
                periodeMedPostering(
                    LocalDate.of(2024, 1, 1),
                    "TPTPATT-OP",
                    -500,
                    fagområde = UtbetalingFagområde.TILTAKSPENGER,
                ),
            )

        val resultat = PosteringStønadstypeMapper.beløpFraUkjentKildePerMåned(perioder)

        assertThat(resultat).isEmpty()
    }

    private fun periodeMedPostering(
        dato: LocalDate,
        klassekode: String,
        beløp: Int,
        type: PosteringType = PosteringType.YTELSE,
        fagområde: UtbetalingFagområde = UtbetalingFagområde.TILLEGGSSTØNADER,
    ) = Periode(
        fom = dato,
        tom = dato,
        posteringer =
            listOf(
                Postering(
                    fagområde = fagområde,
                    sakId = "1",
                    fom = dato,
                    tom = dato,
                    beløp = beløp,
                    type = type,
                    klassekode = klassekode,
                ),
            ),
    )
}
