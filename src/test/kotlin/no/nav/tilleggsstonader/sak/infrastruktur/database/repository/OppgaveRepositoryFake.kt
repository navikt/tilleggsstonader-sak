package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveBehandlingMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import java.util.UUID

class OppgaveRepositoryFake :
    DummyRepository<OppgaveDomain, UUID>({ it.id }),
    OppgaveRepository {
    override fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ): OppgaveDomain? {
        TODO("Not yet implemented")
    }

    override fun findByType(oppgavetype: Oppgavetype): List<OppgaveDomain> {
        TODO("Not yet implemented")
    }

    override fun findByBehandlingIdAndType(
        behandlingId: BehandlingId,
        oppgavetype: Oppgavetype,
    ): List<OppgaveDomain>? {
        TODO("Not yet implemented")
    }

    override fun findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
        behandlingId: BehandlingId,
        oppgavetype: Set<Oppgavetype>,
    ): OppgaveDomain? {
        TODO("Not yet implemented")
    }

    override fun findByGsakOppgaveId(gsakOppgaveId: Long): OppgaveDomain? {
        TODO("Not yet implemented")
    }

    override fun findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId: BehandlingId): OppgaveDomain? {
        TODO("Not yet implemented")
    }

    override fun findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(
        behandlingId: BehandlingId,
        type: Oppgavetype,
    ): OppgaveDomain? {
        TODO("Not yet implemented")
    }

    override fun finnOppgaveMetadata(oppgaveIder: Collection<Long>): List<OppgaveBehandlingMetadata> {
        TODO("Not yet implemented")
    }
}
