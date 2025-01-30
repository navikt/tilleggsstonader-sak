package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.EksternId
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.util.saksbehandling

class BehandlingRepositoryFake :
    DummyRepository<Behandling, BehandlingId>({ it.id }),
    BehandlingRepository {
    override fun findByFagsakId(fagsakId: FagsakId): List<Behandling> {
        TODO("Not yet implemented")
    }

    override fun findByFagsakIdAndStatus(
        fagsakId: FagsakId,
        status: BehandlingStatus,
    ): List<Behandling> {
        TODO("Not yet implemented")
    }

    override fun existsByFagsakId(fagsakId: FagsakId): Boolean {
        TODO("Not yet implemented")
    }

    override fun finnMedEksternId(eksternId: Long): Behandling? {
        TODO("Not yet implemented")
    }

    override fun finnAktivIdent(behandlingId: BehandlingId): String {
        TODO("Not yet implemented")
    }

    override fun finnSaksbehandling(behandlingId: BehandlingId): Saksbehandling = saksbehandling()

    override fun finnSaksbehandling(eksternBehandlingId: Long): Saksbehandling {
        TODO("Not yet implemented")
    }

    override fun finnSisteIverksatteBehandling(fagsakId: FagsakId): Behandling? {
        TODO("Not yet implemented")
    }

    override fun finnBehandlingerForGjenbrukAvVilkår(fagsakPersonId: FagsakPersonId): List<Behandling> {
        TODO("Not yet implemented")
    }

    override fun existsByFagsakIdAndStatusIsNot(
        fagsakId: FagsakId,
        behandlingStatus: BehandlingStatus,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun existsByFagsakIdAndStatusIsNotIn(
        fagsakId: FagsakId,
        behandlingStatus: List<BehandlingStatus>,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun finnEksterneIder(behandlingId: Set<BehandlingId>): Set<EksternId> {
        TODO("Not yet implemented")
    }

    override fun finnSisteIverksatteBehandlingerForPersonIdenter(
        personidenter: Collection<String>,
        stønadstype: Stønadstype,
    ): List<Pair<String, BehandlingId>> {
        TODO("Not yet implemented")
    }

    override fun findAllByStatusAndResultatIn(
        status: BehandlingStatus,
        resultat: List<BehandlingResultat>,
    ): List<Behandling> {
        TODO("Not yet implemented")
    }

    override fun finnGjeldendeIverksatteBehandlinger(stønadstype: Stønadstype): List<Behandling> {
        TODO("Not yet implemented")
    }

    override fun antallGjeldendeIverksatteBehandlinger(stønadstype: Stønadstype): Int {
        TODO("Not yet implemented")
    }
}
