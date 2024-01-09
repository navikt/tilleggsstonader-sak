package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakIdRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.fagsak.domain.tilFagsakMedPerson
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Profile("integrasjonstest")
@Service
class TestoppsettService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakRepository: FagsakRepository,
    private val eksternFagsakIdRepository: EksternFagsakIdRepository,
    private val behandlingRepository: BehandlingRepository,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
) {

    fun hentBehandling(behandlingId: UUID) = behandlingRepository.findByIdOrThrow(behandlingId)

    fun opprettBehandlingMedFagsak(
        behandling: Behandling,
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    ): Behandling {
        val person = opprettPerson(fagsak())
        lagreFagsak(
            fagsak(
                id = behandling.fagsakId,
                stønadstype = stønadstype,
                fagsakPersonId = person.id,
            ),
        )
        return lagre(behandling)
    }

    fun opprettPerson(ident: String) = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(ident))))

    fun opprettPerson(person: FagsakPerson) = fagsakPersonRepository.insert(person)

    fun lagre(behandling: List<Behandling>) {
        behandling.forEach(this::lagre)
    }

    fun lagre(behandling: Behandling): Behandling {
        val dbBehandling = behandlingRepository.insert(behandling)
        eksternBehandlingIdRepository.insert(EksternBehandlingId(behandlingId = dbBehandling.id))
        return dbBehandling
    }

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        val fagsak = fagsakRepository.insert(
            FagsakDomain(
                id = fagsak.id,
                fagsakPersonId = person.id,
                stønadstype = fagsak.stønadstype,
                sporbar = fagsak.sporbar,
            ),
        )
        val eksternFagsakId = eksternFagsakIdRepository.insert(EksternFagsakId(fagsakId = fagsak.id))
        return fagsak.tilFagsakMedPerson(person.identer, eksternFagsakId)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): FagsakPerson {
        return fagsakPersonRepository.findByIdOrNull(fagsak.fagsakPersonId)
            ?: hentPersonFraIdenter(fagsak)
            ?: opprettPerson(fagsak)
    }

    private fun hentPersonFraIdenter(fagsak: Fagsak): FagsakPerson? =
        fagsak.personIdenter.map { it.ident }
            .takeIf { it.isNotEmpty() }
            ?.let { fagsakPersonRepository.findByIdent(it) }

    private fun opprettPerson(fagsak: Fagsak) = fagsakPersonRepository.insert(
        FagsakPerson(
            fagsak.fagsakPersonId,
            identer = fagsak.personIdenter,
        ),
    )
}
