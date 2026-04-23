package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.util.enumTilVisningsnavn

interface BehandlingSteg<T> {
    fun validerSteg(saksbehandling: Saksbehandling) {}

    /**
     * Hvis man trenger å overridea vanlige flytet og returnere en annen stegtype kan man overridea denne metoden,
     * hvis ikke kalles utførSteg uten å returnere en stegType
     */
    fun utførOgReturnerNesteSteg(
        saksbehandling: Saksbehandling,
        data: T,
        kanBehandlePrivatBil: Boolean = false,
    ): StegType {
        utførSteg(saksbehandling, data)
        return nesteSteg(saksbehandling, kanBehandlePrivatBil)
    }

    fun nesteSteg(
        saksbehandling: Saksbehandling,
        kanBehandlePrivatBil: Boolean,
    ): StegType {
        if (saksbehandling.type == BehandlingType.KJØRELISTE) {
            return stegType().hentNesteStegKjørelistebehandling()
        }

        return stegType().hentNesteSteg(saksbehandling.stønadstype)
    }

    fun utførSteg(
        saksbehandling: Saksbehandling,
        data: T,
    )

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
    INNGANGSVILKÅR(
        rekkefølge = 1,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES),
    ),
    VILKÅR(
        rekkefølge = 2,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    BEREGNE_YTELSE(
        rekkefølge = 3,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    KJØRELISTE(
        rekkefølge = 4,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    BEREGNING(
        rekkefølge = 5,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    SIMULERING(
        rekkefølge = 6,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    FULLFØR_KJØRELISTE(
        rekkefølge = 7,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    SEND_TIL_BESLUTTER(
        rekkefølge = 8,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES),
    ),
    BESLUTTE_VEDTAK(
        rekkefølge = 9,
        tillattFor = BehandlerRolle.BESLUTTER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FATTER_VEDTAK),
    ),
    JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV(
        rekkefølge = 10,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    JOURNALFØR_OG_DISTRIBUER_KJØRELISTEBREV(
        rekkefølge = 11,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    FERDIGSTILLE_BEHANDLING(
        rekkefølge = 12,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK),
    ),
    BEHANDLING_FERDIGSTILT(
        rekkefølge = 13,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FERDIGSTILT),
    ),
    ;

    fun visningsnavn(): String = enumTilVisningsnavn(name)

    fun kommerEtter(steg: StegType): Boolean = this.rekkefølge > steg.rekkefølge

    fun erGyldigIKombinasjonMedStatus(behandlingStatus: BehandlingStatus): Boolean =
        this.gyldigIKombinasjonMedStatus.contains(behandlingStatus)

    fun hentNesteSteg(stønadstype: Stønadstype): StegType =
        when (this) {
            INNGANGSVILKÅR -> finnNesteStegInngangsvilkår(stønadstype)
            VILKÅR -> BEREGNE_YTELSE
            BEREGNE_YTELSE -> SIMULERING
            SIMULERING -> SEND_TIL_BESLUTTER
            SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
            BESLUTTE_VEDTAK -> JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV
            JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
            FULLFØR_KJØRELISTE -> FERDIGSTILLE_BEHANDLING
            FERDIGSTILLE_BEHANDLING -> BEHANDLING_FERDIGSTILT
            BEHANDLING_FERDIGSTILT -> BEHANDLING_FERDIGSTILT

            // Steg relevante i revurdering av daglige reiser med bil
            KJØRELISTE -> BEREGNING
            BEREGNING -> SIMULERING

            else -> error("Finner ikke neste steg etter ${this.visningsnavn()}")
        }

    fun hentNesteStegKjørelistebehandling(): StegType =
        when (this) {
            KJØRELISTE -> BEREGNING
            BEREGNING -> SIMULERING
            SIMULERING -> FULLFØR_KJØRELISTE
            FULLFØR_KJØRELISTE -> JOURNALFØR_OG_DISTRIBUER_KJØRELISTEBREV
            JOURNALFØR_OG_DISTRIBUER_KJØRELISTEBREV -> FERDIGSTILLE_BEHANDLING
            FERDIGSTILLE_BEHANDLING -> BEHANDLING_FERDIGSTILT
            BEHANDLING_FERDIGSTILT -> BEHANDLING_FERDIGSTILT
            else -> error("Steg ${this.visningsnavn()} er ikke et gyldig steg for en kjørelistebehandling")
        }

    private fun finnNesteStegInngangsvilkår(stønadstype: Stønadstype): StegType =
        when (stønadstype) {
            Stønadstype.LÆREMIDLER -> BEREGNE_YTELSE
            else -> VILKÅR
        }
}
