package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.gjelderBarn
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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

    fun hentGrunnlagsdata(behandlingId: BehandlingId): Grunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandlingId)

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
        return Grunnlag(
            navn =
                person.søker.navn
                    .gjeldende()
                    .tilNavn(),
            fødsel = mapFødsel(person),
            barn = mapBarn(behandling, person),
            arena = hentGrunnlagArena(behandling),
        )
    }

    private fun hentGrunnlagArena(behandling: Saksbehandling): GrunnlagArena {
        val statusArena = arenaService.hentStatus(behandling.ident, behandling.stønadstype)
        return GrunnlagArenaMapper.mapFaktaArena(statusArena, behandling.stønadstype)
    }

    private fun mapFødsel(person: SøkerMedBarn): Fødsel {
        val fødsel = person.søker.fødselsdato.gjeldende()
        return Fødsel(
            fødselsdato = fødsel.fødselsdato,
            fødselsår = fødsel.fødselsår ?: error("Forventer at fødselsår skal finnes på alle brukere"),
        )
    }

    private fun mapBarn(
        behandling: Saksbehandling,
        person: SøkerMedBarn,
    ): List<GrunnlagBarn> {
        val barnIdenter = barnService.finnBarnPåBehandling(behandling.id).map { it.ident }.toSet()
        val barn = person.barn.filter { (ident, _) -> barnIdenter.contains(ident) }

        feilHvis(
            !barn.keys.containsAll(barnIdenter),
            sensitivFeilmelding = { "Finner ikke grunnlag for barn. behandlingBarn=$barnIdenter pdlBarn=${barn.keys}" },
        ) {
            "Finner ikke grunnlag for barn. Se securelogs for detaljer."
        }

        return barn.tilGrunnlagsdataBarn()
    }

    private fun hentPerson(behandling: Saksbehandling) =
        when (behandling.stønadstype.gjelderBarn()) {
            true -> personService.hentPersonMedBarn(behandling.ident)
            false -> personService.hentPersonUtenBarn(behandling.ident)
        }
}
