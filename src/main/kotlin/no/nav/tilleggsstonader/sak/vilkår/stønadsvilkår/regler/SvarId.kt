package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

enum class SvarId(val beskrivelse: String) {

    // Felles
    JA("Ja"),
    NEI("Nei"),

    // PASS_BARN
    FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID("Ja, tiltak/utdanningssted har dokumentert at søker er borte fra hjemmet utover vanlig arbeidstid"),
    TRENGER_MER_TILSYN_ENN_JEVNALDRENDE("Ja, legeerklæring viser at barnet har behov for vesentlig mer pleie/tilsyn"),
}
