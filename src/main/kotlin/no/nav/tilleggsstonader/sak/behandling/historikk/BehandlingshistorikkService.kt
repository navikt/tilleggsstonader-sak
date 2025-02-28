package no.nav.tilleggsstonader.sak.behandling.historikk

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.BehandlingshistorikkRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.tilHendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.tilJson
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.Hendelse
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.HendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import org.springframework.stereotype.Service

@Service
class BehandlingshistorikkService(
    private val behandlingshistorikkRepository: BehandlingshistorikkRepository,
) {
    fun finnHendelseshistorikk(saksbehandling: Saksbehandling): List<HendelseshistorikkDto> {
        val (hendelserOpprettet, andreHendelser) =
            behandlingshistorikkRepository
                .findByBehandlingIdOrderByEndretTidDesc(
                    saksbehandling.id,
                ).map {
                    it.tilHendelseshistorikkDto(saksbehandling)
                }.filter {
                    it.hendelse != Hendelse.UKJENT
                }.partition { it.hendelse == Hendelse.OPPRETTET }
        val sisteOpprettetHendelse = hendelserOpprettet.lastOrNull()
        return if (sisteOpprettetHendelse != null) {
            andreHendelser + sisteOpprettetHendelse
        } else {
            andreHendelser
        }
    }

    fun finnSisteBehandlingshistorikk(behandlingId: BehandlingId): Behandlingshistorikk =
        behandlingshistorikkRepository.findTopByBehandlingIdOrderByEndretTidDesc(behandlingId)

    fun finnSisteBehandlingshistorikk(
        behandlingId: BehandlingId,
        type: StegType,
    ): Behandlingshistorikk? = behandlingshistorikkRepository.findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId, type)

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingshistorikkRepository.insert(behandlingshistorikk)
    }

    /**
     * @param metadata json object that will be serialized
     */
    fun opprettHistorikkInnslag(
        behandlingId: BehandlingId,
        stegtype: StegType,
        utfall: StegUtfall?,
        metadata: Any?,
    ) {
        opprettHistorikkInnslag(
            Behandlingshistorikk(
                behandlingId = behandlingId,
                steg = stegtype,
                utfall = utfall,
                metadata =
                    metadata?.let {
                        JsonWrapper(objectMapper.writeValueAsString(it))
                    },
                gitVersjon = Applikasjonsversjon.versjon,
            ),
        )
    }

    fun slettFritekstMetadataVedFerdigstillelse(behandlingId: BehandlingId) {
        val relevanteHistorikkinnslag =
            behandlingshistorikkRepository
                .findByBehandlingIdAndStegIn(
                    behandlingId,
                    listOf(StegType.SEND_TIL_BESLUTTER, StegType.BESLUTTE_VEDTAK),
                )

        relevanteHistorikkinnslag.forEach { historikkinnslag ->
            val oppdatertHistorikkinnslag = historikkinnslag.slettFritekstmetadata()
            behandlingshistorikkRepository.update(oppdatertHistorikkinnslag)
        }
    }

    private fun Behandlingshistorikk.slettFritekstmetadata(): Behandlingshistorikk =
        this.copy(
            metadata =
                metadata?.tilJson()?.let {
                    val metadataMap = it.toMutableMap()

                    when (this.steg) {
                        StegType.SEND_TIL_BESLUTTER -> metadataMap.remove("kommentarTilBeslutter")
                        StegType.BESLUTTE_VEDTAK -> metadataMap.remove("begrunnelse")
                        else -> metadataMap
                    }

                    JsonWrapper(objectMapper.writeValueAsString(metadataMap))
                },
        )
}
