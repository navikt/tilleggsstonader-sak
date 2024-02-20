package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle

interface BehandlingSteg<T> {

    fun validerSteg(saksbehandling: Saksbehandling) {}

    /**
     * Hvis man trenger å overridea vanlige flytet og returnere en annen stegtype kan man overridea denne metoden,
     * hvis ikke kalles utførSteg uten å returnere en stegType
     */
    fun utførOgReturnerNesteSteg(saksbehandling: Saksbehandling, data: T): StegType {
        utførSteg(saksbehandling, data)
        return nesteSteg()
    }

    fun nesteSteg() = stegType().hentNesteSteg()

    fun utførSteg(saksbehandling: Saksbehandling, data: T)

    fun stegType(): StegType

    /**
     * Setter om StegService skal sette inn historikk for steget.
     * Hvis den settes til false så må Steget selv legge in historikk
     */
    fun settInnHistorikk() = true
}

enum class StegType(
    val rekkefølge: Int,
    val tillattFor: BehandlerRolle,
    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>,
) {
    REVURDERING_ÅRSAK(
        rekkefølge = 0,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES),
    ),
    VILKÅR(
        rekkefølge = 1,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES),
    ),
    BEREGNE_YTELSE(
        rekkefølge = 2,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    SEND_TIL_BESLUTTER(
        rekkefølge = 3,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    BESLUTTE_VEDTAK(
        rekkefølge = 4,
        tillattFor = BehandlerRolle.BESLUTTER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FATTER_VEDTAK),
    ),
    JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV(
        rekkefølge = 6,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    FERDIGSTILLE_BEHANDLING(
        rekkefølge = 7,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    PUBLISER_VEDTAKSHENDELSE(
        rekkefølge = 8,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FERDIGSTILT),
    ),
    BEHANDLING_FERDIGSTILT(
        rekkefølge = 9,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FERDIGSTILT),
    ),
    ;

    fun displayName(): String {
        return this.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    fun kommerEtter(steg: StegType): Boolean {
        return this.rekkefølge > steg.rekkefølge
    }

    fun erGyldigIKombinasjonMedStatus(behandlingStatus: BehandlingStatus): Boolean {
        return this.gyldigIKombinasjonMedStatus.contains(behandlingStatus)
    }

    fun hentNesteSteg(): StegType {
        return when (this) {
            REVURDERING_ÅRSAK -> VILKÅR
            VILKÅR -> BEREGNE_YTELSE
            BEREGNE_YTELSE -> SEND_TIL_BESLUTTER
            SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
            BESLUTTE_VEDTAK -> JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV
            JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
            /*
            VENTE_PÅ_STATUS_FRA_UTBETALING -> LAG_SAKSBEHANDLINGSBLANKETT
            LAG_SAKSBEHANDLINGSBLANKETT -> FERDIGSTILLE_BEHANDLING
             */
            FERDIGSTILLE_BEHANDLING -> PUBLISER_VEDTAKSHENDELSE
            PUBLISER_VEDTAKSHENDELSE -> BEHANDLING_FERDIGSTILT
            BEHANDLING_FERDIGSTILT -> BEHANDLING_FERDIGSTILT
        }
    }
}
