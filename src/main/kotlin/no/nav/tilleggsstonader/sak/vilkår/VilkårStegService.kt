package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.dto.OppdaterVilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår.utledBehandlingKategori
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår.utledResultatForVilkårSomGjelderFlereBarn
import no.nav.tilleggsstonader.sak.vilkår.regler.hentVilkårsregel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårStegService(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårRepository: VilkårRepository,
    // private val stegService: StegService,
    // private val taskService: TaskService,
    // private val blankettRepository: BlankettRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
) {

    @Transactional
    fun oppdaterVilkår(vilkårsvurderingDto: SvarPåVurderingerDto): VilkårDto {
        val vilkårsvurdering = vilkårRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerLåstForVidereRedigering(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)

        val nyVilkårsvurdering = OppdaterVilkår.lagNyOppdatertVilkår(
            vilkårsvurdering,
            vilkårsvurderingDto.delvilkårsvurderinger,
        )
        // blankettRepository.deleteById(behandlingId)
        val oppdatertVilkårsvurderingDto = vilkårRepository.update(nyVilkårsvurdering).tilDto()
        oppdaterStegOgKategoriPåBehandling(vilkårsvurdering.behandlingId)
        return oppdatertVilkårsvurderingDto
    }

    @Transactional
    fun nullstillVilkår(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårDto {
        val vilkårsvurdering = vilkårRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        // blankettRepository.deleteById(behandlingId)

        val nullstillVilkårMedNyeHovedregler = nullstillVilkårMedNyeHovedregler(behandlingId, vilkårsvurdering)
        oppdaterStegOgKategoriPåBehandling(behandlingId)
        return nullstillVilkårMedNyeHovedregler
    }

    @Transactional
    fun settVilkårTilSkalIkkeVurderes(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårDto {
        val vilkårsvurdering = vilkårRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        // blankettRepository.deleteById(behandlingId)

        val oppdatertVilkår = oppdaterVilkårsvurderingTilSkalIkkeVurderes(behandlingId, vilkårsvurdering)
        oppdaterStegOgKategoriPåBehandling(behandlingId)
        return oppdatertVilkår
    }

    private fun oppdaterStegOgKategoriPåBehandling(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val lagredeVilkårsvurderinger = vilkårRepository.findByBehandlingId(behandlingId)

        oppdaterStegPåBehandling(saksbehandling, lagredeVilkårsvurderinger)
        oppdaterKategoriPåBehandling(saksbehandling, lagredeVilkårsvurderinger)
    }

    private fun oppdaterStegPåBehandling(saksbehandling: Saksbehandling, vilkårsvurderinger: List<Vilkår>) {
        val vilkårsresultat = vilkårsvurderinger.groupBy { it.type }.map {
            if (it.key.gjelderFlereBarn()) {
                utledResultatForVilkårSomGjelderFlereBarn(it.value)
            } else {
                it.value.single().resultat
            }
        }

        if (saksbehandling.steg == StegType.VILKÅR && OppdaterVilkår.erAlleVilkårTattStillingTil(vilkårsresultat)) {
            // stegService.håndterVilkår(saksbehandling).id
        } else if (saksbehandling.steg != StegType.VILKÅR && vilkårsresultat.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL }) {
            // stegService.resetSteg(saksbehandling.id, StegType.VILKÅR)
        } else if (saksbehandling.harStatusOpprettet) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
            behandlingshistorikkService.opprettHistorikkInnslag(
                behandlingId = saksbehandling.id,
                stegtype = StegType.VILKÅR,
                utfall = StegUtfall.UTREDNING_PÅBEGYNT,
                metadata = null,
            )
            opprettBehandlingsstatistikkTask(saksbehandling)
        }
    }

    private fun oppdaterKategoriPåBehandling(
        saksbehandling: Saksbehandling,
        vilkårsvurderinger: List<Vilkår>,
    ) {
        val lagretKategori = saksbehandling.kategori
        val utledetKategori = utledBehandlingKategori(vilkårsvurderinger)

        if (lagretKategori != utledetKategori) {
            behandlingService.oppdaterKategoriPåBehandling(saksbehandling.id, utledetKategori)
        }
    }

    private fun opprettBehandlingsstatistikkTask(saksbehandling: Saksbehandling) {
        // taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = saksbehandling.id))
    }

    private fun nullstillVilkårMedNyeHovedregler(
        behandlingId: UUID,
        vilkår: Vilkår,
    ): VilkårDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkår.type).initiereDelvilkårsvurdering(metadata)
        val delvilkårsvurdering = DelvilkårWrapper(nyeDelvilkår)
        return vilkårRepository.update(
            vilkår.copy(
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                delvilkårwrapper = delvilkårsvurdering,
                opphavsvilkår = null,
            ),
        ).tilDto()
    }

    private fun oppdaterVilkårsvurderingTilSkalIkkeVurderes(
        behandlingId: UUID,
        vilkår: Vilkår,
    ): VilkårDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkår.type).initiereDelvilkårsvurdering(
            metadata,
            Vilkårsresultat.SKAL_IKKE_VURDERES,
        )
        val delvilkårsvurdering = DelvilkårWrapper(nyeDelvilkår)
        return vilkårRepository.update(
            vilkår.copy(
                resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                delvilkårwrapper = delvilkårsvurdering,
                opphavsvilkår = null,
            ),
        ).tilDto()
    }

    private fun hentHovedregelMetadata(behandlingId: UUID) =
        vilkårService.hentGrunnlagOgMetadata(behandlingId).second

    private fun validerLåstForVidereRedigering(behandlingId: UUID) {
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw ApiFeil("Behandlingen er låst for videre redigering", HttpStatus.BAD_REQUEST)
        }
    }

    /**
     * Tilgangskontroll sjekker att man har tilgang til behandlingId som blir sendt inn, men det er mulig å sende inn
     * en annen behandlingId enn den som er på vilkåret
     */
    private fun validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId: UUID, requestBehandlingId: UUID) {
        if (behandlingId != requestBehandlingId) {
            throw Feil(
                "BehandlingId=$requestBehandlingId er ikke lik vilkårets sin behandlingId=$behandlingId",
                "BehandlingId er feil, her har noe gått galt",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
        behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()
}
