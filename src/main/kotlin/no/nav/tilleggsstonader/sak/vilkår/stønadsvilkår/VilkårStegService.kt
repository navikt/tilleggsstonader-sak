package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.hentVilkårsregel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårStegService(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårRepository: VilkårRepository,
) {

    @Transactional
    fun oppdaterVilkår(svarPåVilkårDto: SvarPåVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(svarPåVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerLåstForVidereRedigering(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, svarPåVilkårDto.behandlingId)

        val oppdatertVilkår = OppdaterVilkår.validerOgOppdatertVilkår(vilkår, svarPåVilkårDto.delvilkårsett)
        return vilkårRepository.update(oppdatertVilkår).tilDto()
    }

    @Transactional
    fun nullstillVilkår(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        return nullstillVilkårMedNyeHovedregler(behandlingId, vilkår)
    }

    @Transactional
    fun settVilkårTilSkalIkkeVurderes(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        return oppdaterVilkårTilSkalIkkeVurderes(behandlingId, vilkår)
    }

    private fun nullstillVilkårMedNyeHovedregler(
        behandlingId: UUID,
        vilkår: Vilkår,
    ): VilkårDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkår.type).initiereDelvilkår(metadata, barnId = vilkår.barnId)
        val delvilkårWrapper = DelvilkårWrapper(nyeDelvilkår)
        return vilkårRepository.update(
            vilkår.copy(
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                delvilkårwrapper = delvilkårWrapper,
                opphavsvilkår = null,
            ),
        ).tilDto()
    }

    private fun oppdaterVilkårTilSkalIkkeVurderes(
        behandlingId: UUID,
        vilkår: Vilkår,
    ): VilkårDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkår.type).initiereDelvilkår(
            metadata,
            Vilkårsresultat.SKAL_IKKE_VURDERES,
        )
        val delvilkårWrapper = DelvilkårWrapper(nyeDelvilkår)
        return vilkårRepository.update(
            vilkår.copy(
                resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                delvilkårwrapper = delvilkårWrapper,
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
