package no.nav.tilleggsstonader.sak.vedtak.domain

object BeregningsgrunnlagUtils {
    /**
     * Beregner antall dager per uke som kan brukes
     * Hvis antall dager fra vedtaksperiode er 1, så kan man maks bruke 1 dag fra aktiviteter
     * Hvis antall dager fra vedtaksperiode er 5, men aktiviteter kun har 2 dager så kan man kun bruke 2 dager
     */
    fun beregnAntallAktivitetsdagerForUke(
        periodeMedDager: PeriodeMedDager,
        aktiviteter: List<PeriodeMedDager>,
    ): Int =
        aktiviteter.filter { it.overlapper(periodeMedDager) }.fold(0) { acc, aktivitet ->
            // Tilgjengelige dager i uke i overlapp mellom vedtaksperiode og aktivitet
            val antallTilgjengeligeDager = minOf(periodeMedDager.antallDager, aktivitet.antallDager)

            trekkFraBrukteDager(periodeMedDager, aktivitet, antallTilgjengeligeDager)

            acc + antallTilgjengeligeDager
        }

    /**
     * Skal ikke kunne bruke en dager fra aktivitet eller vedtaksperiode flere ganger.
     * Trekker fra tilgjengelige dager fra antallDager
     * Dersom vedtaksperiode har 2 dager, og aktiviten 3 dager, så skal man kun totalt kunne bruke 2 dager
     * Dersom vedtaksperiode har 3 dager, og aktiviten 2 dager, så skal man kun totalt kunne bruke 2 dager
     */
    private fun trekkFraBrukteDager(
        vedtaksperiode: PeriodeMedDager,
        aktivitet: PeriodeMedDager,
        antallTilgjengeligeDager: Int,
    ) {
        aktivitet.antallDager -= antallTilgjengeligeDager
        vedtaksperiode.antallDager -= antallTilgjengeligeDager
    }
}
