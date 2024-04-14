package no.nav.tilleggsstonader.sak.fagsak.søk

import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.dto.NavnDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * EF har et flyt der man oppretter en fagsak på en person hvis den ikke har fagsak fra før, hvis den:
 * * Finnes i infotrygd
 */
@Service
class SøkService(
    private val fagsakPersonService: FagsakPersonService,
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val arenaService: ArenaService,
) {

    fun søkPersonForEksternFagsak(eksternFagsakId: Long): Søkeresultat {
        val fagsak = fagsakService.hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId)
            ?: throw ApiFeil("Finner ikke fagsak for eksternFagsakId=$eksternFagsakId", HttpStatus.BAD_REQUEST)
        val fagsakPerson = fagsakPersonService.hentPerson(fagsak.fagsakPersonId)
        return tilSøkeresultat(fagsakPerson.hentAktivIdent(), fagsakPerson)
    }

    fun søkPerson(personIdenter: PdlIdenter): Søkeresultat {
        brukerfeilHvis(personIdenter.identer.isEmpty()) {
            "Finner ingen personer for valgt personident"
        }
        val gjeldendePersonIdent = personIdenter.gjeldende().ident

        val fagsakPerson = fagsakPersonService.finnPerson(personIdenter.identer())
        if (fagsakPerson != null) {
            return tilSøkeresultat(gjeldendePersonIdent, fagsakPerson)
        }
        if (arenaService.harSaker(gjeldendePersonIdent)) {
            return tilSøkeresultat(gjeldendePersonIdent, null)
        }

        throw ApiFeil("Personen har ikke fagsak eller sak i arena", HttpStatus.BAD_REQUEST)
    }

    private fun tilSøkeresultat(
        gjeldendePersonIdent: String,
        fagsakPerson: FagsakPerson?,
    ): Søkeresultat {
        val person = personService.hentSøker(gjeldendePersonIdent)

        return Søkeresultat(
            personIdent = gjeldendePersonIdent,
            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
            fagsakPersonId = fagsakPerson?.id,
        )
    }

    fun søkPersonUtenFagsak(personIdent: String): SøkeresultatUtenFagsak {
        return personService.hentPersonKortBolk(listOf(personIdent))[personIdent]?.let {
            SøkeresultatUtenFagsak(
                personIdent = personIdent,
                navn = it.navn.gjeldende().visningsnavn(),
            )
        } ?: throw ApiFeil("Finner ingen personer for søket", HttpStatus.BAD_REQUEST)
    }
}
