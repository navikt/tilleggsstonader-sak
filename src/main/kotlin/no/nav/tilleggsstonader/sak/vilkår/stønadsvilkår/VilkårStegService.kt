package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.vilkår.VilkårSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårsvurderingJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDtoGammel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VurderingJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.utledResultatForVilkårSomGjelderFlereBarn
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
    private val stegService: StegService,
    private val vilkårSteg: VilkårSteg,
    // private val taskService: TaskService,
    // private val blankettRepository: BlankettRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
) {

    @Transactional
    fun oppdaterVilkår(svarPåVilkårDto: SvarPåVilkårDto): VilkårDtoGammel {
        val vilkår = vilkårRepository.findByIdOrThrow(svarPåVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerLåstForVidereRedigering(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, svarPåVilkårDto.behandlingId)

        val oppdatertVilkår = OppdaterVilkår.validerOgOppdatertVilkår(vilkår, svarPåVilkårDto.delvilkårsett)
        // blankettRepository.deleteById(behandlingId)
        val oppdatertVilkårDto = vilkårRepository.update(oppdatertVilkår).tilDto()
        oppdaterStegPåBehandling(vilkår.behandlingId)
        return oppdatertVilkårDto
    }

    private fun mapTilDelvilkårsett(
        vilkår: Vilkår,
        oppdateringer: List<VurderingJson>,
    ): List<DelvilkårDto> {
        return vilkår.delvilkårsett.map { delvilkår ->
            delvilkår.copy(vurderinger = delvilkår.vurderinger.map { vurdering ->
                val oppdatertVurdering = oppdateringer.find { it.regel == vurdering.regelId }
                if (oppdatertVurdering != null) {
                    vurdering.copy(
                        svar = oppdatertVurdering.svar,
                        begrunnelse = oppdatertVurdering.begrunnelse
                    )
                } else {
                    vurdering
                }
            }).tilDto()
        }
    }

    private fun mapTilDelvilkårsett2(
        vilkår: Vilkår,
        oppdateringer: List<VurderingJson>,
    ): List<DelvilkårDto> {

        val resultat = mutableListOf<DelvilkårDto>()

        val stønadsregler = vilkårsreglerPassBarn()
        val delvilkårsett = vilkår.delvilkårsett

        for ((regelId, regelSteg) in stønadsregler) {
            //Bygg et nytt sett med vurderinger

            val relaterteOppdateringer = oppdateringer.find { it.regel == regelId }!!

            val svar = relaterteOppdateringer.svar
            val begrunnelse = relaterteOppdateringer.begrunnelse

            val svarMapping = regelSteg.svarMapping
            val svaralternativ = svarMapping[svar]!!


            // Anta først at vi bare har hovedregler.

            // Vi kan lage nytt Delvilkår, som har resultat satt til IKKE_TATT_STILLING_TIL (som er defaulten)


            val oppdaterteVurderinger: Vurdering = Vurdering(
                regelId = regelId,
                svar = relaterteOppdateringer.svar,
                begrunnelse = relaterteOppdateringer.begrunnelse
            )

            val nyttDelvilkår =
                Delvilkår(vurderinger = listOf(oppdaterteVurderinger)) // En liste med bare ett element, siden vi antar at vi bare har hovedregler nå.

            resultat.add(nyttDelvilkår.tilDto())


            // val relatertDelvilkår = delvilkårsett.find { it.vurderinger.map { vurdering -> vurdering.regelId }.contains(regelId) }


            // val skalHaOppfølging = ...
            // if (skalHaOppfølging) {
            // ...}

        }

        return resultat
    }

    @Transactional
    fun oppdaterVilkårsvurdering(oppdateringer: OppdaterVilkårsvurderingJson): VilkårJson {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdateringer.id)
        val behandlingId = vilkår.behandlingId

        validerLåstForVidereRedigering(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdateringer.behandlingId)

        val delvikårsettetSomErSendtInn = mapTilDelvilkårsett(vilkår, oppdateringer.vurdering)

        val oppdatertVilkår = OppdaterVilkår.validerOgOppdatertVilkår(vilkår, delvikårsettetSomErSendtInn)

        val oppdatertVilkårDto = vilkårRepository.update(oppdatertVilkår).tilJson()

        oppdaterStegPåBehandling(vilkår.behandlingId)

        return oppdatertVilkårDto
    }

    @Transactional
    fun nullstillVilkår(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDtoGammel {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        // blankettRepository.deleteById(behandlingId)

        val oppdatertVilkår = nullstillVilkårMedNyeHovedregler(behandlingId, vilkår)
        oppdaterStegPåBehandling(behandlingId)
        return oppdatertVilkår
    }

    @Transactional
    fun settVilkårTilSkalIkkeVurderes(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDtoGammel {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        // blankettRepository.deleteById(behandlingId)

        val oppdatertVilkår = oppdaterVilkårTilSkalIkkeVurderes(behandlingId, vilkår)
        oppdaterStegPåBehandling(behandlingId)
        return oppdatertVilkår
    }

    private fun oppdaterStegPåBehandling(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val vilkårsett = vilkårRepository.findByBehandlingId(behandlingId)

        oppdaterStegPåBehandling(saksbehandling, vilkårsett)
    }

    private fun oppdaterStegPåBehandling(saksbehandling: Saksbehandling, vilkårsett: List<Vilkår>) {
        val vilkårsresultat = vilkårsett.groupBy { it.type }.map {
            if (it.key.gjelderFlereBarn()) {
                utledResultatForVilkårSomGjelderFlereBarn(it.value)
            } else {
                it.value.single().resultat
            }
        }

        if (saksbehandling.steg == StegType.VILKÅR && OppdaterVilkår.erAlleVilkårTattStillingTil(vilkårsresultat)) {
            stegService.håndterSteg(saksbehandling, vilkårSteg)
        } else if (saksbehandling.steg != StegType.VILKÅR && vilkårsresultat.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL }) {
            stegService.resetSteg(saksbehandling.id, StegType.VILKÅR)
        } else if (saksbehandling.harStatusOpprettet) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
            // TODO vurder denne koblet til når en behandling endrer status etc
            behandlingshistorikkService.opprettHistorikkInnslag(
                behandlingId = saksbehandling.id,
                stegtype = StegType.VILKÅR,
                utfall = StegUtfall.UTREDNING_PÅBEGYNT,
                metadata = null,
            )
            opprettBehandlingsstatistikkTask(saksbehandling)
        }
    }

    private fun opprettBehandlingsstatistikkTask(saksbehandling: Saksbehandling) {
        // taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = saksbehandling.id))
    }

    private fun nullstillVilkårMedNyeHovedregler(
        behandlingId: UUID,
        vilkår: Vilkår,
    ): VilkårDtoGammel {
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
    ): VilkårDtoGammel {
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
