package no.nav.tilleggsstonader.sak.fagsak

import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.dto.FagsakDto
import no.nav.tilleggsstonader.sak.fagsak.dto.tilDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(private val fagsakPersonService: FagsakPersonService, private val fagsakRepository: FagsakRepository) {
    @Transactional
    fun hentEllerOpprettFagsak(
        personIdent: String,
        stønadstype: Stønadstype,
    ): FagsakDto {
        // val personIdenter = personService.hentPersonIdenter(personIdent)
        // val gjeldendePersonIdent = personIdenter.gjeldende()
        // val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val person = fagsakPersonService.hentEllerOpprettPerson(setOf(personIdent), personIdent)
        // val oppdatertPerson = oppdatertPerson(person, gjeldendePersonIdent)
        val fagsak = fagsakRepository.findByFagsakPersonIdAndStønadstype(person.id, stønadstype)
            ?: opprettFagsak(stønadstype, person)

        return fagsak.tilDto(person)
    }
    private fun opprettFagsak(stønadstype: Stønadstype, fagsakPerson: FagsakPerson): FagsakDomain {
        return fagsakRepository.insert(
            FagsakDomain(
                stønadstype = stønadstype,
                fagsakPersonId = fagsakPerson.id,
            ),
        )
    }
}
