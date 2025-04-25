package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.gjelderBarn
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagUtil.ofType
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagUtil.singleOfType
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.BehandlingsinformasjonAnnenForelder
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagArenaVedtak
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagBarnAndreForeldreSaksinformasjon
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagBarnAndreForeldreSaksinformasjonMapper
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagData
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagPersonopplysninger
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.TypeFaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Familierelasjonsrolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FaktaGrunnlagService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val faktaGrunnlagRepository: FaktaGrunnlagRepository,
    private val barnService: BarnService,
    private val personService: PersonService,
    private val vedtakRepository: VedtakRepository,
    private val arenaService: ArenaService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettGrunnlagHvisDetIkkeEksisterer(behandlingId: BehandlingId): FaktaGrunnlagOpprettResultat {
        if (faktaGrunnlagRepository.existsByBehandlingId(behandlingId)) {
            return FaktaGrunnlagOpprettResultat.IkkeOpprettet
        }
        logger.info("Oppretter faktaGrunnlag for behandling=$behandlingId")

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        // TODO dette burde kun gjøres hvis behandlingen er redigerbar men akkurat nå gjøres dette fra BehandlingController som er greit
        opprettGrunnlagPersonopplysninger(behandling)
        opprettGrunnlagBarnAnnenForelder(behandling)
        opprettGrunnlagArenaVedtak(behandling)
        return FaktaGrunnlagOpprettResultat.Opprettet
    }

    fun hentGrunnlagsdata(behandlingId: BehandlingId): Grunnlag {
        val typer =
            listOf(
                TypeFaktaGrunnlag.PERSONOPPLYSNINGER,
                TypeFaktaGrunnlag.ARENA_VEDTAK_TOM,
                TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON,
            )
        val grunnlag = hentGrunnlag(behandlingId, typer)
        return Grunnlag(
            personopplysninger = grunnlag.singleOfType<FaktaGrunnlagPersonopplysninger>().data,
            arenaVedtak = grunnlag.singleOfType<FaktaGrunnlagArenaVedtak>().data,
            saksinformasjonAndreForeldre = grunnlag.ofType<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>(),
        )
    }

    final inline fun <reified TYPE : FaktaGrunnlagData> hentGrunnlag(behandlingId: BehandlingId): List<GeneriskFaktaGrunnlag<TYPE>> =
        hentGrunnlag(behandlingId, TypeFaktaGrunnlag.finnType(TYPE::class))
            .map { faktaGrunnlag -> faktaGrunnlag.withTypeOrThrow<TYPE>() }

    final inline fun <reified TYPE : FaktaGrunnlagData> hentEnkeltGrunnlag(behandlingId: BehandlingId): GeneriskFaktaGrunnlag<TYPE> =
        hentGrunnlag<TYPE>(behandlingId).single()

    fun hentGrunnlag(
        behandlingId: BehandlingId,
        type: TypeFaktaGrunnlag,
    ): List<GeneriskFaktaGrunnlag<out FaktaGrunnlagData>> = faktaGrunnlagRepository.findByBehandlingIdAndType(behandlingId, type)

    fun hentGrunnlag(
        behandlingId: BehandlingId,
        typer: List<TypeFaktaGrunnlag>,
    ): List<GeneriskFaktaGrunnlag<out FaktaGrunnlagData>> = faktaGrunnlagRepository.findByBehandlingIdAndTypeIn(behandlingId, typer)

    private fun opprettGrunnlagPersonopplysninger(behandling: Saksbehandling) {
        val person = hentPerson(behandling)
        val behandlingBarn = barnService.finnBarnPåBehandling(behandling.id)
        lagreFaktaGrunnlag(
            behandling.id,
            FaktaGrunnlagPersonopplysninger.Companion.fraSøkerMedBarn(person, behandlingBarn),
        )
    }

    private fun hentPerson(behandling: Saksbehandling) =
        when (behandling.stønadstype.gjelderBarn()) {
            true -> personService.hentPersonMedBarn(behandling.ident)
            false -> personService.hentPersonUtenBarn(behandling.ident)
        }

    private fun opprettGrunnlagBarnAnnenForelder(behandling: Saksbehandling) {
        if (behandling.stønadstype != Stønadstype.BARNETILSYN) {
            return
        }
        val barnIdenter = barnService.finnBarnPåBehandling(behandling.id).map { it.ident }.toSet()

        val barnAnnenForelder = finnAnnenForelderTilBarn(barnIdenter, behandling)

        val behandlingsinformasjonAnnenForelder = finnBehandlingsinformasjonAnnenForelder(barnAnnenForelder)

        faktaGrunnlagRepository.insertAll(
            FaktaGrunnlagBarnAndreForeldreSaksinformasjonMapper.mapBarnAndreForeldreSaksinformasjon(
                behandling.id,
                barnAnnenForelder,
                behandlingsinformasjonAnnenForelder,
            ),
        )
    }

    private fun opprettGrunnlagArenaVedtak(behandling: Saksbehandling) {
        val statusArena = arenaService.hentStatus(behandling.ident, behandling.stønadstype)
        val vedtakArena = FaktaGrunnlagArenaVedtak.Companion.map(statusArena, behandling.stønadstype)
        lagreFaktaGrunnlag(behandling.id, vedtakArena)
    }

    private fun lagreFaktaGrunnlag(
        behandlingId: BehandlingId,
        data: FaktaGrunnlagData,
    ) {
        val grunnlag =
            GeneriskFaktaGrunnlag(
                behandlingId = behandlingId,
                data = data,
                typeId = null,
            )
        faktaGrunnlagRepository.insert(grunnlag)
    }

    private fun finnBehandlingsinformasjonAnnenForelder(barnAnnenForelder: Map<String, List<String>>) =
        fagsakService
            .finnFagsaker(barnAnnenForelder.values.flatten().toSet())
            .filter { it.stønadstype == Stønadstype.BARNETILSYN }
            .map { fagsak ->
                val iverksattBehandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id)
                BehandlingsinformasjonAnnenForelder(
                    identForelder = fagsak.hentAktivIdent(),
                    finnesIkkeFerdigstiltBehandling = behandlingService.finnesIkkeFerdigstiltBehandling(fagsak.id),
                    iverksattBehandling = iverksattBehandling?.iverksattBehandlingAnnenForelder(),
                )
            }

    private fun Behandling.iverksattBehandlingAnnenForelder(): BehandlingsinformasjonAnnenForelder.IverksattBehandlingForelder {
        val vedtak =
            vedtakRepository
                .findByIdOrThrow(this.id)
                .withTypeOrThrow<InnvilgelseEllerOpphørTilsynBarn>()
        return BehandlingsinformasjonAnnenForelder.IverksattBehandlingForelder(
            barn = barnService.finnBarnPåBehandling(id).associate { it.id to it.ident },
            vedtak = vedtak.data,
        )
    }

    private fun finnAnnenForelderTilBarn(
        barnIdenter: Set<String>,
        behandling: Saksbehandling,
    ): Map<String, List<String>> =
        personService
            .hentBarn(barnIdenter.toList())
            .map { (ident, barn) -> ident to identerAndreForeldre(barn, behandling) }
            .toMap()

    private fun identerAndreForeldre(
        barn: PdlBarn,
        behandling: Saksbehandling,
    ) = barn.forelderBarnRelasjon
        .filter { relasjon -> relasjon.minRolleForPerson == Familierelasjonsrolle.BARN }
        .filter { relasjon -> relasjon.relatertPersonsIdent != behandling.ident }
        .mapNotNull { relasjon -> relasjon.relatertPersonsIdent }
}
