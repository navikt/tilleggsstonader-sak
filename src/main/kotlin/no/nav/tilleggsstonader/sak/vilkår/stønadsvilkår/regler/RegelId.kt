package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

enum class RegelId(
    val beskrivelse: String,
) {
    SLUTT_NODE("SLUTT_NODE"),

    // EKSEMPEL
    HAR_ET_NAVN("Har et navn"),
    HAR_ET_NAVN2("Har et navn 2"),
    HAR_ET_NAVN3("Har et navn 3"),

    // PASSBARN
    ANNEN_FORELDER_MOTTAR_STØTTE("Mottar den andre forelderen støtte til pass av barnet?"),
    UTGIFTER_DOKUMENTERT("Har bruker dokumenterte utgifter til pass?"),
    HAR_FULLFØRT_FJERDEKLASSE("Er barnet ferdig med 4. skoleår?"),
    UNNTAK_ALDER("Har barnet behov for pass utover 4. skoleår, og er behovet tilfredsstillende dokumentert?"),
}
