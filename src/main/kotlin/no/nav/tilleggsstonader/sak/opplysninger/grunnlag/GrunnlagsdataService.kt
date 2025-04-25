package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.gjelderBarn
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.tilNavn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GrunnlagsdataService(
    private val behandlingService: BehandlingService,
    private val barnService: BarnService,
    private val personService: PersonService,
    private val grunnlagsdataRepository: GrunnlagsdataRepository,
    private val faktaGrunnlagService: FaktaGrunnlagService,
    private val arenaService: ArenaService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentGrunnlagsdata(behandlingId: BehandlingId): Grunnlag = grunnlagsdataRepository.findByIdOrThrow(behandlingId).grunnlag

    @Transactional
    fun opprettGrunnlagsdataHvisDetIkkeEksisterer(behandlingId: BehandlingId) {
        if (!grunnlagsdataRepository.existsById(behandlingId)) {
            opprettGrunnlagsdata(behandlingId)
            faktaGrunnlagService.opprettGrunnlag(behandlingId)
        }
    }

    private fun opprettGrunnlagsdata(behandlingId: BehandlingId): Grunnlagsdata {
        // TODO historikk behandling påbegynt NAV-20376
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        logger.info("Oppretter grunnlagsdata for behandling=$behandlingId status=${behandling.status}")

        val grunnlag = hentGrunnlagFraRegister(behandling)
        val grunnlagsdata =
            Grunnlagsdata(
                behandlingId = behandlingId,
                grunnlag = grunnlag,
            )
        grunnlagsdataRepository.insert(grunnlagsdata)
        return grunnlagsdata
    }

    private fun hentGrunnlagFraRegister(behandling: Saksbehandling): Grunnlag {
        val person = hentPerson(behandling)
        val behandlingBarn = barnService.finnBarnPåBehandling(behandling.id)
        return Grunnlag(
            navn =
                person.søker.navn
                    .gjeldende()
                    .tilNavn(),
            fødsel = FødselFaktaGrunnlag.fraSøkerMedBarn(person),
            barn = GrunnlagBarn.fraSøkerMedBarn(person, behandlingBarn),
            arena = hentGrunnlagArena(behandling),
        )
    }

    private fun hentGrunnlagArena(behandling: Saksbehandling): GrunnlagArena {
        val statusArena = arenaService.hentStatus(behandling.ident, behandling.stønadstype)
        return GrunnlagArenaMapper.mapFaktaArena(statusArena, behandling.stønadstype)
    }

    private fun hentPerson(behandling: Saksbehandling) =
        when (behandling.stønadstype.gjelderBarn()) {
            true -> personService.hentPersonMedBarn(behandling.ident)
            false -> personService.hentPersonUtenBarn(behandling.ident)
        }
}
