package no.nav.tilleggsstonader.sak.cucumber

interface Domenenøkkel {
    val nøkkel: String
}

enum class DomenenøkkelFelles(
    override val nøkkel: String,
) : Domenenøkkel {
    FOM("Fom"),
    TOM("Tom"),
    BELØP("Beløp"),
}
