package no.nav.tilleggsstonader.sak.behandling.historikk

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.BehandlingshistorikkRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.tilHendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.Hendelse
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.HendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingshistorikkService(private val behandlingshistorikkRepository: BehandlingshistorikkRepository) {

    fun finnHendelseshistorikk(saksbehandling: Saksbehandling): List<HendelseshistorikkDto> {
        val (hendelserOpprettet, andreHendelser) = behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(
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

    fun finnSisteBehandlingshistorikk(behandlingId: UUID): Behandlingshistorikk {
        return behandlingshistorikkRepository.findTopByBehandlingIdOrderByEndretTidDesc(behandlingId)
    }

    fun finnSisteBehandlingshistorikk(behandlingId: UUID, type: StegType): Behandlingshistorikk? =
        behandlingshistorikkRepository.findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId, type)

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingshistorikkRepository.insert(behandlingshistorikk)
    }

    /**
     * @param metadata json object that will be serialized
     */
    fun opprettHistorikkInnslag(
        behandlingId: UUID,
        stegtype: StegType,
        utfall: StegUtfall?,
        metadata: Any?,
    ) {
        opprettHistorikkInnslag(
            Behandlingshistorikk(
                behandlingId = behandlingId,
                steg = stegtype,
                utfall = utfall,
                metadata = metadata?.let {
                    JsonWrapper(objectMapper.writeValueAsString(it))
                },
            ),
        )
    }
}
