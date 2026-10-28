package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

enum class SvarId(
    val beskrivelse: String,
) {
    // Felles
    JA("Ja"),
    NEI("Nei"),

    // FLYTTING
    FLYTTING_VED_OPPSTART_ELLER_AVSLUTNING_AV_TILTAK_ELLER_UTDANNING(
        "Flytting ved oppstart eller avslutning av tiltak eller utdanning",
    ),
    FLYTTER_TIL_AKTIVITETSSTED("Ja, flytter til aktivitetssted"),
    FLYTTER_TIL_HJEMSTED("Ja, flytter til hjemsted"),
    ARBEID_ETTER_GJENNOMFØRT_AKTIVITET("Flytting i forbindelse med arbeid etter gjennomført tiltak eller utdanning"),
    ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID(
        "Enslig forsørger eller gjenlevende som flytter fra hjemstedet for å starte i arbeid",
    ),
    FLYTTEBYRÅ("Flyttebyrå"),
    FLYTTER_SELV("Flytter selv"),

    // PASS_BARN
    FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID(
        "Ja, tiltak/utdanningssted har dokumentert at søker er borte fra hjemmet utover vanlig arbeidstid",
    ),
    TRENGER_MER_TILSYN_ENN_JEVNALDRENDE("Ja, legeerklæring viser at barnet har behov for vesentlig mer pleie/tilsyn"),

    // ROAH
    ROAH_HAR_BARN_IKKE_FERDIG_MED_FJERDE_SKOLEÅR("Har barn som bor hjemme, som ikke er ferdig med fjerde skoleår"),
    ROAH_HAR_DOKUMENTERTE_ANDRE_SÆRSKILTE_BEHOV("Har dokumentert andre særskilte behov for flere hjemreiser"),
}
