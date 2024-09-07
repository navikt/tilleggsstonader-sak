package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BarnService(
    private val barnRepository: BarnRepository,
) {

    @Transactional
    fun opprettBarn(barn: List<BehandlingBarn>): List<BehandlingBarn> =
        barnRepository.insertAll(barn)

    fun finnBarnPåBehandling(behandlingId: BehandlingId): List<BehandlingBarn> =
        barnRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun gjenbrukBarn(
        forrigeBehandlingId: BehandlingId,
        nyBehandlingId: BehandlingId,
    ): Map<TidligereBarnId, NyttBarnId> {
        val nyeBarnPåGammelId = barnRepository.findByBehandlingId(forrigeBehandlingId)
            .associate { it.id to it.copy(id = UUID.randomUUID(), behandlingId = nyBehandlingId, sporbar = Sporbar()) }
        barnRepository.insertAll(nyeBarnPåGammelId.values.toList())
        return nyeBarnPåGammelId.map { it.key to it.value.id }.toMap()
    }
}

/**
 * Typer for å få litt tydeligere grensesnitt på [gjenbrukBarn]
 */
typealias TidligereBarnId = UUID
typealias NyttBarnId = UUID

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
