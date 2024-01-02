package no.nav.tilleggsstonader.sak.opplysninger

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.opplysninger.dto.NavnDto
import no.nav.tilleggsstonader.sak.opplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(
    private val fagsakPersonService: FagsakPersonService,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
) {

    // TODO denne burde hente fra grunnlag?
    fun hentPersonopplysninger(behandlingId: UUID): PersonopplysningerDto {
        return hentPersonopplysninger(behandlingService.hentAktivIdent(behandlingId))
    }

    fun hentPersonopplysningerForFagsakPerson(fagsakPersonId: UUID): PersonopplysningerDto {
        return hentPersonopplysninger(fagsakPersonService.hentAktivIdent(fagsakPersonId))
    }

    private fun hentPersonopplysninger(ident: String): PersonopplysningerDto {
        val pdlSøker = personService.hentSøker(ident)
        return PersonopplysningerDto(
            personIdent = ident,
            navn = pdlSøker.navn.gjeldende().let { NavnDto.fraNavn(it) },
        )
    }
}
