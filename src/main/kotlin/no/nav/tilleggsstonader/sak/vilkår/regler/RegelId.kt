package no.nav.tilleggsstonader.sak.vilkår.regler

enum class RegelId(val beskrivelse: String) {
    SLUTT_NODE("SLUTT_NODE"),

    // EKSEMPEL
    HAR_ET_NAVN("Har et navn"),
    HAR_ET_NAVN2("Har et navn 2"),
    HAR_ET_NAVN3("Har et navn 3"),

    // MÅLGRUPPE
    MÅLGRUPPE("Tilhører bruker riktig målgruppe?"),

    // AKTIVITET
    ER_AKTIVITET_REGISTRERT("Er bruker registrert med en aktivitet?"),
}
