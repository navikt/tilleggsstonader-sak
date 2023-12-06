package no.nav.tilleggsstonader.sak.vilkår.regler

enum class RegelId(val beskrivelse: String) {
    SLUTT_NODE("SLUTT_NODE"),

    // EKSEMPEL
    HAR_ET_NAVN("Har et navn"),
    HAR_ET_NAVN2("Har et navn 2"),
    HAR_ET_NAVN3("Har et navn 3"),

    // MÅLGRUPPE
    MÅLGRUPPE("Tilhører bruker riktig målgruppe?"),
    NEDSATT_ARBEIDSEVNE("Har bruker nedsatt arbeidsevne etter §11 A-3?"),

    // AKTIVITET
    ER_AKTIVITET_REGISTRERT("Er bruker registrert med en aktivitet?"),
    LØNN_GJENNOM_TILTAK("Mottar bruker lønn gjennom tiltak?"),
    MOTTAR_SYKEPENGER_GJENNOM_AKTIVITET("Mottar bruker sykepenger gjennom aktivitet?"),

    // PASSBARN
    DEKKES_UTGIFTER_ANNET_REGELVERK("Dekkes utgifter av annet regelverk?"),
    ANNEN_FORELDER_MOTTAR_STØTTE("Mottar den andre forelderen støtte for dette barnet?"),
    UTGIFTER_DOKUMENTERT("Er utgiftene tilfredstillende dokumentert?"),
    HAR_ALDER_LAVERE_ENN_GRENSEVERDI("Har barnet fullført 4. skoleår?"),
    UNNTAK_ALDER("Oppfylles unntak etter å ha fullført 4. skoleår?"),
}
