package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.fagsak.domain.tilFagsakMedPerson
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class TestoppsettService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
) {

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
        return behandlingRepository.insert(behandling)
    }

    fun opprettPerson(ident: String) = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(ident))))

    fun opprettPerson(person: FagsakPerson) = fagsakPersonRepository.insert(person)

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        return fagsakRepository.insert(
            FagsakDomain(
                id = fagsak.id,
                fagsakPersonId = person.id,
                stønadstype = fagsak.stønadstype,
                eksternId = fagsak.eksternId,
                sporbar = fagsak.sporbar,
            ),
        ).tilFagsakMedPerson(person.identer)
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
