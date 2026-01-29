package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

data class BeregningsresultatDagligReise(
    val offentligTransport: BeregningsresultatOffentligTransport?,
    val privatBil: DummyBeregningsresultatPrivatBil?,
)

// TODO: Erstatt med ekte dataklasse
// Kun lagt inn for å håndtere at privat bil må legges inn en del steder.
data class DummyBeregningsresultatPrivatBil(
    val resultat: Int,
)
