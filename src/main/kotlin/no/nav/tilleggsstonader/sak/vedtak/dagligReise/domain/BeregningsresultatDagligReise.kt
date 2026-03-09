package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

data class BeregningsresultatDagligReise(
    val offentligTransport: BeregningsresultatOffentligTransport?,
    val privatBil: BeregningsresultatPrivatBil?,
)
