package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

enum class SvarId(
    val beskrivelse: String,
) {
    // Felles
    JA("Ja"),
    NEI("Nei"),

    // FLYTTING
    FLYTTING_VED_START_AV_AKTIVITET("Flytting ved start av aktivitet"),
    FLYTTING_VED_AVSLUTTET_AKTIVITET("Flytting ved avsluttet aktivitet"),
    ARBEID_ETTER_GJENNOMFØRT_AKTIVITET("Arbeid etter gjennomført tiltak/utdanning"),
    ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID(
        "Enlig forsørger eller gjenlevende som flytter fra hjemstedet for å starte i arbeid",
    ),
    FLYTTEBYRÅ("Flyttebyrå"),
    FLYTTER_SELV("Flytter selv"),

    // PASS_BARN
    FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID(
        "Ja, tiltak/utdanningssted har dokumentert at søker er borte fra hjemmet utover vanlig arbeidstid",
    ),
    TRENGER_MER_TILSYN_ENN_JEVNALDRENDE("Ja, legeerklæring viser at barnet har behov for vesentlig mer pleie/tilsyn"),
}
