package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GrunnlagsdataService(
    private val behandlingService: BehandlingService,
    private val barnService: BarnService,
    private val personService: PersonService,
    private val grunnlagsdataRepository: GrunnlagsdataRepository,
    private val arenaService: ArenaService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentGrunnlagsdata(behandlingId: UUID): Grunnlagsdata {
        return grunnlagsdataRepository.findByIdOrThrow(behandlingId)
    }

    fun opprettGrunnlagsdataHvisDetIkkeEksisterer(behandlingId: UUID) {
        if (!grunnlagsdataRepository.existsById(behandlingId)) {
            opprettGrunnlagsdata(behandlingId)
        }
    }

    private fun opprettGrunnlagsdata(behandlingId: UUID): Grunnlagsdata {
        // TODO historikk behandling påbegynt NAV-20376
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        logger.info("Oppretter grunnlagsdata for behandling=$behandlingId status=${behandling.status}")

        val grunnlag = hentGrunnlagFraRegister(behandling)
        val grunnlagsdata = Grunnlagsdata(
            behandlingId = behandlingId,
            grunnlag = grunnlag,
        )
        grunnlagsdataRepository.insert(grunnlagsdata)
        return grunnlagsdata
    }

    private fun hentGrunnlagFraRegister(behandling: Saksbehandling): Grunnlag {
        val person = hentPerson(behandling)
        return Grunnlag(
            navn = person.søker.navn.gjeldende().tilNavn(),
            fødsel = mapFødsel(person),
            barn = mapBarn(behandling, person),
            arena = hentGrunnlagArena(behandling),
        )
    }

    private fun hentGrunnlagArena(behandling: Saksbehandling): GrunnlagArena {
        val statusArena = arenaService.hentStatus(behandling.ident, behandling.stønadstype)
        return GrunnlagArenaMapper.mapFaktaArena(statusArena)
    }

    private fun mapFødsel(person: SøkerMedBarn): Fødsel {
        val fødsel = person.søker.fødsel.gjeldende()
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

        feilHvis(!barn.keys.containsAll(barnIdenter)) {
            "Finner ikke grunnlag for barn. behandlingBarn=$barnIdenter pdlBarn=${barn.keys}"
        }

        return barn.tilGrunnlagsdataBarn()
    }

    private fun hentPerson(behandling: Saksbehandling) = when (behandling.stønadstype) {
        Stønadstype.BARNETILSYN -> personService.hentPersonMedBarn(behandling.ident)
        else -> personService.hentPersonUtenBarn(behandling.ident)
    }
}
