package no.nav.tilleggsstonader.sak.journalføring.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import java.time.LocalDate
import java.util.UUID

data class JournalføringRequest(
    val dokumentTitler: Map<String, String>? = null,
    val logiskeVedlegg: Map<String, List<LogiskVedlegg>>? = null,
    val ident: String,
    val stønadstype: Stønadstype,
    val oppgaveId: String,
    val journalførendeEnhet: String, // TODO: Hvorfor sendes denne inn?
    val årsak: Journalføringsårsak,
    val aksjon: Journalføringsaksjon,
    val mottattDato: LocalDate? = null, // Brukes av klage
    val nyAvsender: NyAvsender? = null,
) {
    fun gjelderKlage(): Boolean {
        return årsak == Journalføringsårsak.KLAGE || årsak == Journalføringsårsak.KLAGE_TILBAKEKREVING
    }

    fun tilUstrukturertDokumentasjonType(): UstrukturertDokumentasjonType {
        return when (årsak) {
            Journalføringsårsak.ETTERSENDING -> UstrukturertDokumentasjonType.ETTERSENDING
            Journalføringsårsak.PAPIRSØKNAD -> UstrukturertDokumentasjonType.PAPIRSØKNAD
            Journalføringsårsak.DIGITAL_SØKNAD, Journalføringsårsak.KLAGE_TILBAKEKREVING, Journalføringsårsak.KLAGE -> UstrukturertDokumentasjonType.IKKE_VALGT
        }
    }

    fun skalJournalføreTilNyBehandling(): Boolean = aksjon == Journalføringsaksjon.OPPRETT_BEHANDLING

    data class NyAvsender(val erBruker: Boolean, val navn: String?, val personIdent: String?)

    enum class Journalføringsaksjon {
        OPPRETT_BEHANDLING,
        JOURNALFØR_PÅ_FAGSAK,
    }

    enum class Journalføringsårsak(val behandlingsårsak: BehandlingÅrsak) {
        KLAGE_TILBAKEKREVING(BehandlingÅrsak.KLAGE),
        KLAGE(BehandlingÅrsak.KLAGE),
        PAPIRSØKNAD(BehandlingÅrsak.PAPIRSØKNAD),
        DIGITAL_SØKNAD(BehandlingÅrsak.SØKNAD),
        ETTERSENDING(BehandlingÅrsak.NYE_OPPLYSNINGER),
    }

    /**
     * [IKKE_VALGT] er indirekte det samme som digital søknad, der man ikke velger ustrukturert dokumentasjonstype
     */
    enum class UstrukturertDokumentasjonType(val behandlingÅrsak: () -> BehandlingÅrsak) {
        PAPIRSØKNAD({ BehandlingÅrsak.PAPIRSØKNAD }),
        ETTERSENDING({ BehandlingÅrsak.NYE_OPPLYSNINGER }),
        IKKE_VALGT({ error("Kan ikke bruke behandlingsårsak fra $IKKE_VALGT") }), ;

        fun erEttersending(): Boolean = this == ETTERSENDING
    }
}

fun JournalføringRequest.valider() {
    feilHvis(gjelderKlage()){
        "Journalføring av klage er ikke implementert."
    }
    dokumentTitler?.let {
        brukerfeilHvis(
            it.containsValue(""),
        ) {
            "Mangler tittel på et eller flere dokumenter"
        }
    }
}
