package no.nav.tilleggsstonader.sak.cucumber

interface Domenenøkkel {
    val nøkkel: String
}

enum class DomenenøkkelFelles(
    override val nøkkel: String,
) : Domenenøkkel {
    ID("Id"),
    FOM("Fom"),
    TOM("Tom"),
    BELØP("Beløp"),
}

enum class DomenenøkkelAndelTilkjentYtelse(
    override val nøkkel: String,
) : Domenenøkkel {
    UTBETALINGSDATO("Utbetalingsdato"),
    TYPE("Type"),
    SATS("Sats"),
    STATUS_IVERKSETTING("Status iverksetting"),
}
