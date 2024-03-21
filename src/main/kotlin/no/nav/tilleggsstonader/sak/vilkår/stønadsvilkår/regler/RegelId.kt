package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

enum class RegelId(val beskrivelse: String) {
    SLUTT_NODE("SLUTT_NODE"),

    // EKSEMPEL
    HAR_ET_NAVN("Har et navn"),
    HAR_ET_NAVN2("Har et navn 2"),
    HAR_ET_NAVN3("Har et navn 3"),

    // PASSBARN
    ANNEN_FORELDER_MOTTAR_STØTTE("Mottar den andre forelderen støtte for dette barnet?"),
    UTGIFTER_DOKUMENTERT("Er utgiftene tilfredstillende dokumentert?"),
    HAR_ALDER_LAVERE_ENN_GRENSEVERDI("Har barnet fullført 4. skoleår?"),
    UNNTAK_ALDER("Oppfylles unntak etter å ha fullført 4. skoleår?"),
}
