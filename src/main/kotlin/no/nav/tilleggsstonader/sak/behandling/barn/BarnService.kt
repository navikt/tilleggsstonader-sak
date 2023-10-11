package no.nav.tilleggsstonader.sak.behandling.barn

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnService(
    private val barnRepository: BarnRepository,
) {

    fun opprettBarn(barn: List<BehandlingBarn>): List<BehandlingBarn> =
        barnRepository.insertAll(barn)

    fun finnBarnPåBehandling(behandlingId: UUID): List<BehandlingBarn> =
        barnRepository.findByBehandlingId(behandlingId)
}

/**
 * Her skal vi opprette barn
 * Gjelder for [BARNETILSYN]
 *  * Skal opprette barn som kommer inn fra søknaden
 *  * Burde kunne opprette barn som blir lagt in manuellt via journalføringen?
 *  * Oppdatere personopplysninger -> oppdatere barn
 *  * Kopiere barn ved revurdering
 *
 *  * Ved revurdering burde vi appende barn, sånn at man fortsatt får med vilkår for tidligere barn?
 */
