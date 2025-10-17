package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingType
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingTypeV2
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.OPPRETTET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OpprettBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val unleashService: UnleashService,
    private val settPåVentService: SettPåVentService,
) {
    @Transactional
    fun opprettBehandling(
        fagsakId: FagsakId,
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        stegType: StegType = StegType.INNGANGSVILKÅR,
        behandlingsårsak: BehandlingÅrsak,
        kravMottatt: LocalDate? = null,
        nyeOpplysningerMetadata: NyeOpplysningerMetadata? = null,
    ): Behandling {
        brukerfeilHvis(kravMottatt != null && kravMottatt.isAfter(LocalDate.now())) {
            "Kan ikke sette krav mottattdato frem i tid"
        }
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_BEHANDLING)) {
            "Feature toggle for å opprette behandling er slått av"
        }

        val kanHaFlereBehandlingerPåSammeFagsak =
            unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK)

        val tidligereBehandlinger = behandlingRepository.findByFagsakId(fagsakId)
        val forrigeBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
        val behandlingType =
            when (kanHaFlereBehandlingerPåSammeFagsak) {
                true -> utledBehandlingTypeV2(tidligereBehandlinger)
                false -> utledBehandlingType(tidligereBehandlinger)
            }

        validerKanOppretteNyBehandling(
            behandlingType = behandlingType,
            tidligereBehandlinger = tidligereBehandlinger,
            kanHaFlereBehandlingPåSammeFagsak = kanHaFlereBehandlingerPåSammeFagsak,
        )

        val behandling =
            behandlingRepository.insert(
                Behandling(
                    fagsakId = fagsakId,
                    forrigeIverksatteBehandlingId = forrigeBehandling?.id,
                    type = behandlingType,
                    steg = stegType,
                    status = status,
                    resultat = BehandlingResultat.IKKE_SATT,
                    årsak = behandlingsårsak,
                    kravMottatt = kravMottatt,
                    kategori = BehandlingKategori.NASJONAL,
                    nyeOpplysningerMetadata = nyeOpplysningerMetadata,
                ),
            )
        eksternBehandlingIdRepository.insert(EksternBehandlingId(behandlingId = behandling.id))

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingshistorikk =
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = stegType,
                    gitVersjon = Applikasjonsversjon.versjon,
                ),
        )

        return behandling
    }
}
