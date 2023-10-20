package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

// TODO burde kanskje kun opprette grunnlag for barn som finnes i behandlingBarn?
/**
 * Denne skal på sikt lagre og hente data fra databasen, men for å ikke begrense seg
 */
@Service
class GrunnlagsdataService(
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
) {

    fun hentFraRegister(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val ident = behandlingService.hentAktivIdent(behandlingId)
        val grunnlagsdata = hentGrunnlagsdata(ident)
        return GrunnlagsdataMedMetadata(
            grunnlagsdata = grunnlagsdata,
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    fun opprettGrunlagsdata(behandlingId: UUID) = NotImplementedError()

    private fun hentGrunnlagsdata(ident: String): Grunnlagsdata {
        val personMedBarn = personService.hentPersonMedBarn(ident)
        return Grunnlagsdata(
            barn = personMedBarn.barn.tilGrunnlagsdataBarn(),
        )
    }
}
