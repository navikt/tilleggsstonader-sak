package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.lagNyttVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.VilkårsresultatUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.finnesVilkårTypeForStønadstype
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.hentVilkårsregel
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårService(
    private val behandlingService: BehandlingService,
    private val vilkårRepository: VilkårRepository,
    private val barnService: BarnService,
    private val behandlingFaktaService: BehandlingFaktaService,
    private val fagsakService: FagsakService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun oppdaterVilkår(svarPåVilkårDto: SvarPåVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(svarPåVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandling(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, svarPåVilkårDto.behandlingId)

        val oppdatertVilkår = flettVilkårOgVurderResultat(vilkår, svarPåVilkårDto)
        return vilkårRepository.update(oppdatertVilkår).tilDto()
    }

    @Transactional
    fun opprettNyttVilkår(opprettVilkårDto: OpprettVilkårDto): Vilkår {
        val behandlingId = opprettVilkårDto.behandlingId

        validerBehandling(behandlingId)
        validerBehandlingOgVilkårType(behandlingId, opprettVilkårDto.vilkårType)

        val relevanteRegler = hentVilkårsregel(opprettVilkårDto.vilkårType)
        val metadata = hentHovedregelMetadata(behandlingId)
        validerBarnFinnesPåBehandling(metadata, opprettVilkårDto)

        val nyttVilkår =
            lagNyttVilkår(
                behandlingId = behandlingId,
                metadata = metadata,
                vilkårsregel = relevanteRegler,
                barnId = opprettVilkårDto.barnId,
            )

        val oppdatertVilkår = flettVilkårOgVurderResultat(nyttVilkår, opprettVilkårDto)

        return vilkårRepository.insert(oppdatertVilkår)
    }

    private fun flettVilkårOgVurderResultat(
        vilkår: Vilkår,
        lagreVilkårDto: LagreVilkårDto,
    ): Vilkår {
        val vurderingsresultat = OppdaterVilkår.validerVilkårOgBeregnResultat(
            vilkår = vilkår,
            oppdatering = lagreVilkårDto,
        )
        val oppdatertVilkår = OppdaterVilkår.oppdaterVilkår(
            vilkår = vilkår,
            oppdatering = lagreVilkårDto,
            vilkårsresultat = vurderingsresultat,
        )
        return oppdatertVilkår
    }

    @Transactional
    fun slettVilkår(oppdaterVilkårDto: OppdaterVilkårDto) {
        val vilkårId = oppdaterVilkårDto.id
        val vilkår = vilkårRepository.findByIdOrThrow(vilkårId)
        val behandlingId = vilkår.behandlingId

        feilHvis(vilkår.opphavsvilkår != null) {
            "Kan ikke slette vilkår opprettet i tidligere behandling"
        }
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerBehandling(behandlingId)

        vilkårRepository.deleteById(vilkårId)
    }

    @Transactional
    fun settVilkårTilSkalIkkeVurderes(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerBehandling(behandlingId)

        return oppdaterVilkårTilSkalIkkeVurderes(behandlingId, vilkår)
    }

    private fun oppdaterVilkårTilSkalIkkeVurderes(
        behandlingId: BehandlingId,
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

    private fun hentHovedregelMetadata(behandlingId: BehandlingId) = hentGrunnlagOgMetadata(behandlingId).second

    private fun validerBehandling(behandlingId: BehandlingId) {
        validerBehandling(behandlingService.hentSaksbehandling(behandlingId))
    }

    private fun validerBehandlingOgVilkårType(behandlingId: BehandlingId, vilkårType: VilkårType) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        validerBehandling(behandling)

        feilHvisIkke(finnesVilkårTypeForStønadstype(behandling.stønadstype, vilkårType)) {
            "Vilkårtype=$vilkårType eksisterer ikke for stønadstype=${behandling.stønadstype}"
        }
    }

    private fun validerBehandling(behandling: Saksbehandling) {
        validerErIVilkårSteg(behandling)
        validerLåstForVidereRedigering(behandling)
    }

    private fun validerBarnFinnesPåBehandling(metadata: HovedregelMetadata, opprettVilkårDto: OpprettVilkårDto) {
        val barnIderPåBehandling = metadata.barn.map { it.id }.toSet()
        feilHvisIkke(barnIderPåBehandling.contains(opprettVilkårDto.barnId)) {
            "Finner ikke barn på behandling"
        }
    }

    private fun validerLåstForVidereRedigering(behandling: Saksbehandling) {
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            throw ApiFeil("Behandlingen er låst for videre redigering", HttpStatus.BAD_REQUEST)
        }
    }

    /**
     * Tilgangskontroll sjekker att man har tilgang til behandlingId som blir sendt inn, men det er mulig å sende inn
     * en annen behandlingId enn den som er på vilkåret
     */
    private fun validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId: BehandlingId, requestBehandlingId: BehandlingId) {
        if (behandlingId != requestBehandlingId) {
            throw Feil(
                "BehandlingId=$requestBehandlingId er ikke lik vilkårets sin behandlingId=$behandlingId",
                "BehandlingId er feil, her har noe gått galt",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validerErIVilkårSteg(behandling: Saksbehandling) {
        brukerfeilHvisIkke(behandling.steg == StegType.VILKÅR) {
            "Kan ikke oppdatere vilkår når behandling er på steg=${behandling.steg}."
        }
    }

    @Transactional
    fun hentVilkårsvurdering(behandlingId: BehandlingId): VilkårsvurderingDto {
        val (grunnlag, metadata) = hentGrunnlagOgMetadata(behandlingId)
        val vurderinger = hentEllerOpprettVilkår(behandlingId, metadata).map(Vilkår::tilDto)
        return VilkårsvurderingDto(vilkårsett = vurderinger, grunnlag = grunnlag)
    }

    fun hentVilkårsett(behandlingId: BehandlingId): List<VilkårDto> {
        val vilkårsett = hentVilkår(behandlingId)
        return vilkårsett.map { it.tilDto() }
    }

    fun hentVilkår(behandlingId: BehandlingId): List<Vilkår> {
        return vilkårRepository.findByBehandlingId(behandlingId)
    }

    @Transactional
    fun oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandlingId: BehandlingId): VilkårsvurderingDto {
        // grunnlagsdataService.oppdaterOgHentNyGrunnlagsdata(behandlingId)
        return this.hentVilkårsvurdering(behandlingId)
    }

    fun hentGrunnlagOgMetadata(behandlingId: BehandlingId): Pair<BehandlingFaktaDto, HovedregelMetadata> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val barn = barnService.finnBarnPåBehandling(behandlingId)
        val grunnlag = behandlingFaktaService.hentFakta(behandlingId)
        return Pair(grunnlag, HovedregelMetadata(barn, behandling))
    }

    // TODO rename metode når man kun henter lagretVilkårsett når FT fjernes
    fun hentEllerOpprettVilkår(
        behandlingId: BehandlingId,
        metadata: HovedregelMetadata,
    ): List<Vilkår> {
        return vilkårRepository.findByBehandlingId(behandlingId)
    }

    /*private fun finnEndringerIGrunnlagsdata(behandlingId: UUID): List<GrunnlagsdataEndring> {
        val oppdaterteGrunnlagsdata = grunnlagsdataService.hentFraRegister(behandlingId)
        val eksisterendeGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        return oppdaterteGrunnlagsdata.endringerMellom(eksisterendeGrunnlagsdata)
    }*/

    /**
     * Når en revurdering opprettes skal den kopiere de tidligere vilkårene for samme stønad.
     */
    fun kopierVilkårsettTilNyBehandling(
        forrigeBehandlingId: BehandlingId,
        nyBehandling: Behandling,
        barnIdMap: Map<TidligereBarnId, NyttBarnId>,
    ) {
        val tidligereVurderinger =
            vilkårRepository.findByBehandlingId(forrigeBehandlingId).associateBy { it.id }

        val kopiAvVurderinger = lagKopiAvTidligereVurderinger(
            tidligereVurderinger,
            nyBehandling.id,
            barnIdMap,
        )

        vilkårRepository.insertAll(kopiAvVurderinger)
    }

    private fun lagKopiAvTidligereVurderinger(
        tidligereVilkår: Map<VilkårId, Vilkår>,
        nyBehandlingsId: BehandlingId,
        barnIdMap: Map<TidligereBarnId, NyttBarnId>,
    ): List<Vilkår> =
        tidligereVilkår.values.map { vilkår ->
            vilkår.copy(
                id = VilkårId.random(),
                behandlingId = nyBehandlingsId,
                sporbar = Sporbar(),
                barnId = finnBarnId(vilkår.barnId, barnIdMap),
                opphavsvilkår = vilkår.opprettOpphavsvilkår(),
            )
        }

    private fun finnBarnId(barnId: BarnId?, barnIdMap: Map<BarnId, BarnId>): BarnId? =
        barnId?.let {
            barnIdMap[it]
                ?: error("Fant ikke barn=$it på gjeldende behandling med barnIdMapping=$barnIdMap")
        }

    fun erAlleVilkårOppfylt(behandlingId: BehandlingId): Boolean {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val lagretVilkårsett = vilkårRepository.findByBehandlingId(behandlingId)
        return VilkårsresultatUtil.erAlleVilkårOppfylt(lagretVilkårsett, stønadstype)
    }

    fun hentOppfyltePassBarnVilkår(behandlingId: BehandlingId): List<Vilkår> {
        return hentPassBarnVilkår(behandlingId)
            .filter { it.resultat == Vilkårsresultat.OPPFYLT }
    }

    fun hentPassBarnVilkår(behandlingId: BehandlingId): List<Vilkår> {
        return vilkårRepository.findByBehandlingId(behandlingId)
            .filter { it.type == VilkårType.PASS_BARN }
    }

    companion object {

        fun byggBarnMapFraTidligereTilNyId(
            barnPåForrigeBehandling: List<BehandlingBarn>,
            barnPåGjeldendeBehandling: List<BehandlingBarn>,
        ): Map<BarnId, BehandlingBarn> {
            val barnFraForrigeBehandlingMap = barnPåForrigeBehandling.associateBy { it.id }.toMutableMap()
            return barnPåGjeldendeBehandling.mapNotNull { nyttBarn ->
                val forrigeBarnId =
                    barnFraForrigeBehandlingMap.entries.firstOrNull { nyttBarn.erMatchendeBarn(it.value) }?.key
                barnFraForrigeBehandlingMap.remove(forrigeBarnId)
                forrigeBarnId?.let { Pair(forrigeBarnId, nyttBarn) }
            }.associate { it.first to it.second }
        }
    }
}
