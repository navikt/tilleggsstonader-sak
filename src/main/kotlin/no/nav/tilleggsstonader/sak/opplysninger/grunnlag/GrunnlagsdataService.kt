package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import org.springframework.stereotype.Service
import java.util.UUID

// TODO burde kanskje kun opprette grunnlag for barn som finnes i behandlingBarn?
/**
 * Denne skal på sikt lagre og hente data fra databasen, men for å ikke begrense seg
 */
@Service
class GrunnlagsdataService(
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val grunnlagsdataRepository: GrunnlagsdataRepository,
) {

    fun opprettGrunnlagsdata(behandlingId: UUID) {
        feilHvis(grunnlagsdataRepository.existsById(behandlingId)) {
            "Grunnlagsdata finnes allerede for behandling=$behandlingId"
        }
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val grunnlagsdata = hentGrunnlagsdataFraRegister(behandling)
        val grunnlagsdataDomain = Grunnlagsdata(
            behandlingId = behandlingId,
            grunnlag = grunnlagsdata,
        )
        grunnlagsdataRepository.insert(grunnlagsdataDomain)
    }

    // TODO når skal vi opprette grunnlagsdata?
    // Skal vi sjekke om den finnes, hvis den ikke finnes og behandling kan behandles - opprett grunnlag?
    fun hentGrunnlagsdata(behandlingId: UUID): Grunnlagsdata {
        return grunnlagsdataRepository.findByIdOrThrow(behandlingId)
    }

    private fun hentGrunnlagsdataFraRegister(behandling: Saksbehandling): Grunnlag {
        val person = hentPerson(behandling)
        return Grunnlag(
            navn = mapNavn(person),
            barn = person.barn.tilGrunnlagsdataBarn(),
        )
    }

    private fun hentPerson(behandling: Saksbehandling) =
        if (behandling.stønadstype == Stønadstype.BARNETILSYN) {
            personService.hentPersonMedBarn(behandling.ident)
        } else {
            personService.hentPersonUtenBarn(behandling.ident)
        }

    private fun mapNavn(personMedBarn: SøkerMedBarn): Navn {
        return personMedBarn.søker.navn.gjeldende()
            .let {
                Navn(
                    fornavn = it.fornavn,
                    mellomnavn = it.mellomnavn,
                    etternavn = it.etternavn,
                )
            }
    }
}
