package no.nav.tilleggsstonader.sak.satsjustering

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FinnBehandlingerForSatsjusteringService(
    private val behandlingRepository: BehandlingRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sjekkBehandlingerForSatsjustering(stønadstype: Stønadstype): List<BehandlingId> {
        val idn = behandlingRepository.finnBehandlingerMedAndelerSomVenterPåSatsjustering(stønadstype)
        logger.info("Finner ${idn.size} behandlinger av stønadstype=$stønadstype som venter på satsjustering.")
        return idn
    }
}
