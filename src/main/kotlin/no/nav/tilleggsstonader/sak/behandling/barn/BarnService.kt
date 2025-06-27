package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BarnService(
    private val barnRepository: BarnRepository,
) {
    @Transactional
    fun opprettBarn(barn: List<BehandlingBarn>): List<BehandlingBarn> = barnRepository.insertAll(barn)

    fun finnBarnPåBehandling(behandlingId: BehandlingId): List<BehandlingBarn> = barnRepository.findByBehandlingId(behandlingId)

    fun finnIdenterTilFagsakPersonId(fagsakPersonId: FagsakPersonId) = barnRepository.finnIdenterTilFagsakPersonId(fagsakPersonId)

    @Transactional
    fun gjenbrukBarn(
        forrigeIverksatteBehandlingId: BehandlingId,
        nyBehandlingId: BehandlingId,
    ): Map<TidligereBarnId, NyttBarnId> {
        val nyeBarnPåGammelId =
            barnRepository
                .findByBehandlingId(forrigeIverksatteBehandlingId)
                .associate { it.id to it.copy(id = BarnId.random(), behandlingId = nyBehandlingId, sporbar = Sporbar()) }
        barnRepository.insertAll(nyeBarnPåGammelId.values.toList())
        return nyeBarnPåGammelId.map { it.key to it.value.id }.toMap()
    }

    fun kopierManglendeBarnFraForrigeBehandling(
        forrigeBehandlingId: BehandlingId,
        nyBehandling: Behandling,
    ) {
        val barnPåForrigeBehandling = finnBarnPåBehandling(forrigeBehandlingId)
        val barnPåBehandlingSomSkalTasAvVent = finnBarnPåBehandling(nyBehandling.id)

        val identerPåVent = barnPåBehandlingSomSkalTasAvVent.map { it.ident }.toSet()
        val barnSomMåLeggesTilBehandlingSomSkalTasAvVent =
            barnPåForrigeBehandling.filter { it.ident !in identerPåVent }

        val nyeBarn =
            barnSomMåLeggesTilBehandlingSomSkalTasAvVent.map {
                it.copy(behandlingId = nyBehandling.id, id = BarnId.random(), sporbar = Sporbar())
            }

        barnRepository.insertAll(nyeBarn)
    }
}

/**
 * Typer for å få litt tydeligere grensesnitt på [gjenbrukBarn]
 */
typealias TidligereBarnId = BarnId
typealias NyttBarnId = BarnId

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
