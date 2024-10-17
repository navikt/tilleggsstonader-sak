package no.nav.tilleggsstonader.sak.opplysninger

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.opplysninger.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.dto.NavnDto
import no.nav.tilleggsstonader.sak.opplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.sak.opplysninger.fullmakt.FullmaktService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import org.springframework.stereotype.Service

@Service
class PersonopplysningerService(
    private val fagsakPersonService: FagsakPersonService,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val fullmaktService: FullmaktService,
) {

    // TODO denne burde hente fra grunnlag?
    fun hentPersonopplysninger(behandlingId: BehandlingId): PersonopplysningerDto {
        return hentPersonopplysninger(behandlingService.hentAktivIdent(behandlingId))
    }

    fun hentPersonopplysningerForFagsakPerson(fagsakPersonId: FagsakPersonId): PersonopplysningerDto {
        return hentPersonopplysninger(fagsakPersonService.hentAktivIdent(fagsakPersonId))
    }

    private fun hentPersonopplysninger(ident: String): PersonopplysningerDto {
        logger.info("Henter personopplysninger")
        val pdlSøker = personService.hentSøker(ident)
        val harFullmektig = fullmaktService.hentFullmektige(ident).isNotEmpty()
        return PersonopplysningerDto(
            personIdent = ident,
            navn = pdlSøker.navn.gjeldende().let { NavnDto.fraNavn(it) },
            harVergemål = pdlSøker.vergemaalEllerFremtidsfullmakt
                .any { it.type != "stadfestetFremtidsfullmakt" }, // fremtidsfullmakt gjelder frem i tiden
            harFullmektig = harFullmektig,
            adressebeskyttelse = Adressebeskyttelse.fraPdl(pdlSøker.adressebeskyttelse.gradering()),
        )
    }
}
