package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakPersonService(
    private val personService: PersonService,
    private val fagsakPersonRepository: FagsakPersonRepository,
) {

    fun hentPerson(personId: UUID): FagsakPerson = fagsakPersonRepository.findByIdOrThrow(personId)

    fun hentPersoner(personId: List<UUID>): Iterable<FagsakPerson> = fagsakPersonRepository.findAllById(personId)

    fun finnPerson(personIdenter: Set<String>): FagsakPerson? = fagsakPersonRepository.findByIdent(personIdenter)

    fun hentIdenter(personId: UUID): Set<PersonIdent> {
        val personIdenter = fagsakPersonRepository.findPersonIdenter(personId)
        feilHvis(personIdenter.isEmpty()) { "Finner ikke personidenter til person=$personId" }
        return personIdenter
    }

    fun hentAktivIdent(personId: UUID): String = fagsakPersonRepository.hentAktivIdent(personId)

    @Transactional
    fun hentEllerOpprettPerson(ident: String): FagsakPerson {
        // En person kan ha flere personnummer, så vi må spørre PDL om det gjeldende.
        val personIdenter = personService.hentPersonIdenter(ident)
        val gjeldendePersonIdent = personIdenter.gjeldende().ident
        return hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent)
    }

    @Transactional
    fun hentEllerOpprettPerson(personIdenter: Set<String>, gjeldendePersonIdent: String): FagsakPerson {
        feilHvisIkke(personIdenter.contains(gjeldendePersonIdent)) {
            "Liste med personidenter inneholder ikke gjeldende personident"
        }
        return (
            fagsakPersonRepository.findByIdent(personIdenter)
                ?: fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(gjeldendePersonIdent))))
            )
    }

    @Transactional
    fun oppdaterIdent(fagsakPerson: FagsakPerson, gjeldendePersonIdent: String): FagsakPerson {
        return if (fagsakPerson.hentAktivIdent() != gjeldendePersonIdent) {
            fagsakPersonRepository.update(fagsakPerson.medOppdatertGjeldendeIdent(gjeldendePersonIdent))
        } else {
            fagsakPerson
        }
    }
}
