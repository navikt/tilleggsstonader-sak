package no.nav.tilleggsstonader.sak.vilkår

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
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår
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
    private val stegService: StegService,
    private val vilkårSteg: VilkårSteg,
    // private val taskService: TaskService,
    // private val blankettRepository: BlankettRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val stønadsperiodeService: StønadsperiodeService,
) {

    @Transactional
    fun oppdaterVilkår(svarPåVilkårDto: SvarPåVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(svarPåVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerLåstForVidereRedigering(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, svarPåVilkårDto.behandlingId)

        val oppdatertVilkår = OppdaterVilkår.validerOgOppdatertVilkår(vilkår, svarPåVilkårDto.delvilkårsett)
        // blankettRepository.deleteById(behandlingId)
        val oppdatertVilkårDto = vilkårRepository.update(oppdatertVilkår).tilDto()
        oppdaterStegPåBehandling(behandlingId, vilkår.type)
        return oppdatertVilkårDto
    }

    @Transactional
    fun nullstillVilkår(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        feilHvis(vilkår.type.gjelderMålgruppeEllerAktivitet()) {
            "Har ikke støtte for å nullstille inngangsvilkår"
        }
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        // blankettRepository.deleteById(behandlingId)

        val oppdatertVilkår = nullstillVilkårMedNyeHovedregler(behandlingId, vilkår)
        oppdaterStegPåBehandling(behandlingId, vilkår.type)
        return oppdatertVilkår
    }

    @Transactional
    fun settVilkårTilSkalIkkeVurderes(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        feilHvis(vilkår.type.gjelderMålgruppeEllerAktivitet()) {
            "Har ikke støtte for å sette inngangsvilkår til 'skal ikke vurderes'"
        }
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        // blankettRepository.deleteById(behandlingId)

        val oppdatertVilkår = oppdaterVilkårTilSkalIkkeVurderes(behandlingId, vilkår)
        oppdaterStegPåBehandling(behandlingId, vilkår.type)
        return oppdatertVilkår
    }

    private fun oppdaterStegPåBehandling(behandlingId: UUID, type: VilkårType) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val vilkårsett = vilkårRepository.findByBehandlingId(behandlingId)

        oppdaterStegPåBehandling(saksbehandling, type, vilkårsett)
    }

    private fun oppdaterStegPåBehandling(saksbehandling: Saksbehandling, type: VilkårType, vilkårsett: List<Vilkår>) {
        /**
         * Hvis det er en endring på gjelderMålgruppeEllerAktivitet så skal vi validere at stønadsperiodene fortsatt er gyldige
         * Hvis de ikke er gyldige burde vi vel resette steget?
         * Hvis de er gyldige så kan vi beholde steget? Eller burde man sette det til beregning, sånn at man må kalle på beregning på nytt som oppretter nye Andeler?
         * Og så etter det må man generere brevet på nytt
         *
         * Burde man då få en varning om at man er i ferd med å miste brev-informasjon?
         *
         * Hvis man endrer vilkår/stønadsperioder generellt så risikerer man ju at vedtaket ikke lengre er gyldig, og då burde man ju fjerne vedtaket generellt?
         * Eks om man går fra innvilget -> avslått? Eller er det noe man skal validere når man er på beregningssiden?
         */
        val vilkårsresultat =
            vilkårsett.filterNot { it.type.gjelderMålgruppeEllerAktivitet() }.groupBy { it.type }.map {
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
    ): VilkårDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkår.type).initiereDelvilkår(metadata)
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
