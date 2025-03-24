package no.nav.tilleggsstonader.sak.opplysninger

import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.opplysninger.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.dto.NavnDto
import no.nav.tilleggsstonader.sak.opplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.fullmakt.FullmaktService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import org.springframework.stereotype.Service

@Service
class PersonopplysningerService(
    private val fagsakPersonService: FagsakPersonService,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val fullmaktService: FullmaktService,
    private val egenAnsattService: EgenAnsattService,
) {
    // TODO denne burde hente fra grunnlag?
    fun hentPersonopplysninger(behandlingId: BehandlingId): PersonopplysningerDto =
        hentPersonopplysninger(behandlingService.hentAktivIdent(behandlingId))

    fun hentPersonopplysningerForFagsakPerson(fagsakPersonId: FagsakPersonId): PersonopplysningerDto =
        hentPersonopplysninger(fagsakPersonService.hentAktivIdent(fagsakPersonId))

    private fun hentPersonopplysninger(ident: String): PersonopplysningerDto {
        val pdlSøker = personService.hentSøker(ident)
        val harFullmektig = fullmaktService.hentFullmektige(ident).isNotEmpty()
        return PersonopplysningerDto(
            personIdent = ident,
            navn = pdlSøker.navn.gjeldende().let { NavnDto.fraNavn(it) },
            harVergemål =
                pdlSøker.vergemaalEllerFremtidsfullmakt
                    .any { it.type != "stadfestetFremtidsfullmakt" },
            // fremtidsfullmakt gjelder frem i tiden
            harFullmektig = harFullmektig,
            adressebeskyttelse = Adressebeskyttelse.fraPdl(pdlSøker.adressebeskyttelse.gradering()),
            erSkjermet = egenAnsattService.erEgenAnsatt(ident),
            dødsdato = pdlSøker.dødsfall.gjeldende()?.dødsdato,
            erUtlandet = erUtlandet(ident),
        )
    }

    private fun erUtlandet(ident: String): Boolean {
        val geografiskTilknytning = personService.hentGeografiskTilknytning(ident)
        return geografiskTilknytning?.gtType == GeografiskTilknytningType.UTLAND
    }
}
