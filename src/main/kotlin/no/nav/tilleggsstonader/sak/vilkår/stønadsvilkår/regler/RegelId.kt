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

    // BOUTGIFTER
    NØDVENDIGE_MERUTGIFTER("Har søker nødvendige merutgifter til bolig eller overnatting?"),
    DOKUMENTERT_UTGIFTER_OVERNATTING("Har søker dokumentert utgifter til overnatting tilfredsstillende?"),
    HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE(
        "Har søker dokumentert høyere boutgifter på aktivitetssted sammenlignet med tidligere bolig?",
    ),
    NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET("Er det nødvendig for søker å bo nærmere aktivitetsstedet?"),
    RETT_TIL_BOSTØTTE("Har søker rett til bostøtte for boligen de søker om støtte til?"),
    HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER("Har søker høyere utgifter grunnet helsemessige årsaker?"),
    DOKUMENTERT_UTGIFTER_BOLIG("Har søker dokumentert utgifter til bolig tilfredsstillende?"),
    DOKUMENTERT_DELTAKELSE("Har søker dokumentert at de har samling/eksamen/opptaksprøve/kurs på datoene for overnatting?"),
}
