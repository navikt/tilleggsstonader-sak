package no.nav.tilleggsstonader.sak.opplysninger.oppgave

/**
 * Vi bruker mapper for oppgaver vi oppretter for at saksbehandlere som har gosys-vakt og
 * skal ta oppgaver i oppgavebenken i gosys ikke skal se oppgaver fra ny saksbehandling.
 *
 * Oppgaver av typen journalføring, behandle sak, behandle underkjent vedtak og behandle vedtak skal i Klar-mappen.
 * På-vent oppgaver skal flyttes til på-vent mappen.
 *
 * Det er då ønskelig at gosys-vakter skal se på uplasserte oppgaver.
 * Hvis en ettersending til tilsyn barn kommer der, så skal den vanligvis flyttes manuellt til klar-mappen,
 * for å sen journalføres/behandles i ny saksbehandling.
 */
enum class OppgaveMappe(vararg val navn: String) {
    KLAR("TS-sak Klar"),
    PÅ_VENT("TS-sak På vent", "TS Sak På vent"),
    ;

    override fun toString() = navn.joinToString(",")
}
