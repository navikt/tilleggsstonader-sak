package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel

enum class BoutgifterDomenenøkkel(
    override val nøkkel: String,
) : Domenenøkkel {
    UTBETALINGSDATO("Utbetalingsdato"),
    UTGIFT("Utgift"),
    MAKS_SATS("Maks sats"),
    STØNADSBELØP("Stønadsbeløp"),
    AKTIVITET("Aktivitet"),
    MÅLGRUPPE("Målgruppe"),
    DEL_AV_TIDLIGERE_UTBETALING("Del av tidligere utbetaling"),
}
