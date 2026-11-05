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
    NØDVENDIGE_MERUTGIFTER("Har søker nødvendige merutgifter til overnatting?"),
    DOKUMENTERT_UTGIFTER_OVERNATTING("Har søker dokumentert utgifter til overnatting tilfredsstillende?"),
    HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE(
        "Har søker dokumentert høyere boutgifter på aktivitetssted sammenlignet med tidligere bolig?",
    ),
    NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET("Er det nødvendig for søker å bo nærmere aktivitetsstedet?"),
    RETT_TIL_BOSTØTTE("Har søker rett til bostøtte for boligen de søker om støtte til?"),
    HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER("Har søker høyere utgifter grunnet helsemessige årsaker?"),
    DOKUMENTERT_UTGIFTER_BOLIG("Har søker dokumentert utgifter til bolig tilfredsstillende?"),
    DOKUMENTERT_DELTAKELSE("Har søker dokumentert at de har samling/eksamen/opptaksprøve/kurs på datoene for overnatting?"),

    // DAGLIG REISE
    AVSTAND_OVER_SEKS_KM("Er reiseavstanden over 6km?"),
    UNNTAK_SEKS_KM(
        "Har søker dokumentert funksjonsnedsettelse, midlertidig skade eller sykdom som gjør at søker har et særskilt behov for transport?",
    ),
    KAN_REISE_MED_OFFENTLIG_TRANSPORT("Kan søker benytte seg av offentlig transport?"),
    KAN_KJØRE_MED_EGEN_BIL("Kan søker benytte privat bil?"),
    KAN_REISE_MED_TAXI("Kan søker benytte seg av taxi?"),

    // REISE TIL SAMLING
    HAR_UTGIFTER_TIL_REISEN("Har bruker utgifter til reisen?"),
    AVSTAND_OVER_TRETTI_KM("Er reiseavstanden over 30km?"),
    ER_SAMLING_OBLIGATORISK("Er samlingen obligatorisk?"),
    KAN_REISE_MED_EGEN_BIL("Kan bruker benytte privat bil?"),
    DOKUMENTERTE_UTGIFTER("Har bruker dokumenterte utgifter til reise?"),
    KAN_REISE_MED_DROSJE("Kan bruker benytte seg av drosje?"),
    HAR_DOKUMENTERTE_UTGIFTER_TIL_DROSJE("Har bruker dokumenterte utgifter til drosje?"),

    // REISE OPPSTART, AVSLUTNING OG HJEMREISE
    ROAH_HAR_UTGIFTER_TIL_REISEN("Har bruker utgifter til reisen?"),
    ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT("Har bruker mulighet til å reise med offentlig transport?"),
    ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN("Har bruker dokumenterte utgifter til reisen?"),
    ROAH_KAN_BENYTTE_PRIVAT_BIL("Kan bruker benytte seg av privat bil?"),
    ROAH_BENYTTET_TAXI("Benyttet bruker taxi?"),
    ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL("Er søker innenfor normalt antall hjemreiser per år?"),
    ROAH_HJEMREISE_BEHOV_FLERE_REISER("Har bruker behov for flere hjemreiser, og er behovet tilfredsstillende dokumentert?"),

    // FLYTTING
    HAR_UTGIFTER_TIL_FLYTTING("Har bruker utgifter til flyttingen?"),
    HVILKEN_FORBINDELSE_FLYTTER_BRUKER("I hvilken forbindelse flytter bruker?"),
    MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET("Må bruker flytte fra/til hjemsted?"),
    FLYTTER_IFBM_ARBEID_ETTER_AKTIVITET("Må bruker flytte i forbindelse med arbeid etter gjennomført tiltak eller utdanning?"),
    MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID("Må bruker flytte fra hjemstedet for å starte i arbeid på et annet sted?"),
    HVORDAN_SKAL_BRUKER_FLYTTE("Skal bruker benytte seg av flyttebyrå, eller flytter bruker selv?"),
}
