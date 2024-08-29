package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.hentVilkårsregel
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
        TODO("Not yet implemented")
    }

    private fun flettVilkårOgVurderResultat(
        vilkår: Vilkår,
        svarPåVilkårDto: SvarPåVilkårDto,
    ): Vilkår {
        val vurderingsresultat = OppdaterVilkår.validerVilkårOgBeregnResultat(
            vilkår = vilkår,
            oppdatering = svarPåVilkårDto.delvilkårsett,
        )
        val oppdatertVilkår = OppdaterVilkår.oppdaterVilkår(
            vilkår = vilkår,
            oppdatering = svarPåVilkårDto.delvilkårsett,
            vilkårsresultat = vurderingsresultat,
        )
        return oppdatertVilkår
    }

    @Transactional
    fun nullstillVilkår(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerBehandling(behandlingId)

        return nullstillVilkårMedNyeHovedregler(behandlingId, vilkår)
    }

    @Transactional
    fun settVilkårTilSkalIkkeVurderes(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        validerBehandling(behandlingId)

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

    private fun hentHovedregelMetadata(behandlingId: UUID) = hentGrunnlagOgMetadata(behandlingId).second

    private fun validerBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerErIVilkårSteg(behandling)
        validerLåstForVidereRedigering(behandling)
    }

    private fun validerLåstForVidereRedigering(behandling: Behandling) {
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
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

    private fun validerErIVilkårSteg(behandling: Behandling) {
        brukerfeilHvisIkke(behandling.steg == StegType.VILKÅR) {
            "Kan ikke oppdatere vilkår når behandling er på steg=${behandling.steg}."
        }
    }

    @Transactional
    fun hentEllerOpprettVilkårsvurdering(behandlingId: UUID): VilkårsvurderingDto {
        val (grunnlag, metadata) = hentGrunnlagOgMetadata(behandlingId)
        val vurderinger = hentEllerOpprettVilkår(behandlingId, metadata).map(Vilkår::tilDto)
        return VilkårsvurderingDto(vilkårsett = vurderinger, grunnlag = grunnlag)
    }

    @Transactional
    fun hentOpprettEllerOppdaterVilkårsvurdering(behandlingId: UUID): VilkårsvurderingDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        if (behandling.harStatusOpprettet) {
            /*val endredeGrunnlagsdata = finnEndringerIGrunnlagsdata(behandlingId)
            if (endredeGrunnlagsdata.isNotEmpty()) {
                secureLogger.info("Grunnlagsdata som har endret seg: $endredeGrunnlagsdata")
                logger.info("Grunnlagsdata har endret seg siden sist. Sletter gamle vilkår og grunnlagsdata og legger inn nye.")
                grunnlagsdataService.oppdaterOgHentNyGrunnlagsdata(behandlingId)
                vilkårsvurderingRepository.deleteByBehandlingId(behandlingId)
            }*/
        }
        return hentEllerOpprettVilkårsvurdering(behandlingId)
    }

    fun hentVilkårsett(behandlingId: UUID): List<VilkårDto> {
        val vilkårsett = vilkårRepository.findByBehandlingId(behandlingId)
        feilHvis(vilkårsett.isEmpty()) { "Mangler vilkår for behandling=$behandlingId" }
        return vilkårsett.map { it.tilDto() }
    }

    fun hentVilkårsresultat(behandlingId: UUID): List<Vilkårsresultat> {
        return vilkårRepository.findByBehandlingId(behandlingId).groupBy { it.type }.map {
            if (it.key.gjelderFlereBarn()) {
                OppdaterVilkår.utledResultatForVilkårSomGjelderFlereBarn(it.value)
            } else {
                it.value.single().resultat
            }
        }
    }

    @Transactional
    fun oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandlingId: UUID): VilkårsvurderingDto {
        // grunnlagsdataService.oppdaterOgHentNyGrunnlagsdata(behandlingId)
        return hentEllerOpprettVilkårsvurdering(behandlingId)
    }

    /*
    @Transactional
    fun opprettVilkårForOmregning(behandling: Behandling) {
        feilHvisIkke(behandling.årsak == BehandlingÅrsak.G_OMREGNING) { "Maskinelle vurderinger kun for G-omregning." }
        val (_, metadata) = hentGrunnlagOgMetadata(behandling.id)
        val stønadstype = fagsakService.hentFagsakForBehandling(behandling.id).stønadstype
        kopierVurderingerTilNyBehandling(
            eksisterendeBehandlingId = behandling.forrigeBehandlingId ?: error("Finner ikke forrige behandlingId"),
            nyBehandlingsId = behandling.id,
            metadata = metadata,
            stønadType = stønadstype,

        )
    }
     */

    fun hentGrunnlagOgMetadata(behandlingId: UUID): Pair<BehandlingFaktaDto, HovedregelMetadata> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val barn = barnService.finnBarnPåBehandling(behandlingId)
        val grunnlag = behandlingFaktaService.hentFakta(behandlingId)
        return Pair(grunnlag, HovedregelMetadata(barn, behandling))
    }

    fun hentEllerOpprettVilkår(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
    ): List<Vilkår> {
        val lagretVilkårsett = vilkårRepository.findByBehandlingId(behandlingId)

        return when {
            behandlingErLåstForVidereRedigering(behandlingId) -> lagretVilkårsett
            lagretVilkårsett.isEmpty() -> lagNyttVilkårsett(behandlingId, metadata)
            else -> hentEllerOpprettMedEvtNyeBarn(metadata, lagretVilkårsett, behandlingId)
        }
    }

    private fun hentEllerOpprettMedEvtNyeBarn(
        metadata: HovedregelMetadata,
        lagretVilkårsett: List<Vilkår>,
        behandlingId: UUID,
    ): List<Vilkår> {
        val harVilkårForAlleBarn =
            lagretVilkårsett.filter { it.type.gjelderFlereBarn() }.map { it.type }.toSet().all { vilkårType ->
                metadata.barn.all { barn ->
                    lagretVilkårsett.any { it.barnId == barn.id && it.type == vilkårType }
                }
            }

        if (!harVilkårForAlleBarn) {
            return lagretVilkårsett + opprettVilkårForNyeBarn(behandlingId, metadata, lagretVilkårsett)
        }

        return lagretVilkårsett
    }

    private fun opprettVilkårForNyeBarn(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
        lagretVilkårsett: List<Vilkår>,
    ): List<Vilkår> {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val nyeVilkår = OppdaterVilkår.opprettVilkårForNyeBarn(behandlingId, metadata, stønadstype, lagretVilkårsett)
        return vilkårRepository.insertAll(nyeVilkår)
    }

    private fun lagNyttVilkårsett(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
    ): List<Vilkår> {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val nyttVilkårsett: List<Vilkår> = opprettNyeVilkår(
            behandlingId = behandlingId,
            metadata = metadata,
            stønadstype = stønadstype,
        )
        return vilkårRepository.insertAll(nyttVilkårsett)
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
        forrigeBehandlingId: UUID,
        nyBehandling: Behandling,
        barnIdMap: Map<TidligereBarnId, NyttBarnId>,
        stønadstype: Stønadstype,
    ) {
        val tidligereVurderinger =
            vilkårRepository.findByBehandlingId(forrigeBehandlingId).associateBy { it.id }

        validerAtVurderingerKanKopieres(tidligereVurderinger, forrigeBehandlingId)

        val kopiAvVurderinger: Map<UUID, Vilkår> = lagKopiAvTidligereVurderinger(
            tidligereVurderinger,
            nyBehandling.id,
            barnIdMap,
        )

        val nyeBarnVurderinger = opprettVilkårForNyeBarn(
            vilkårKopi = kopiAvVurderinger,
            nyBehandling = nyBehandling,
            stønadstype = stønadstype,
        )

        vilkårRepository.insertAll(kopiAvVurderinger.values.toList() + nyeBarnVurderinger)
    }

    private fun validerAtVurderingerKanKopieres(
        tidligereVurderinger: Map<UUID, Vilkår>,
        forrigeBehandlingId: UUID,
    ) {
        if (tidligereVurderinger.isEmpty()) {
            val melding = "Tidligere behandling=$forrigeBehandlingId har ikke noen vilkår"
            throw Feil(melding, melding)
        }
    }

    private fun lagKopiAvTidligereVurderinger(
        tidligereVilkår: Map<UUID, Vilkår>,
        nyBehandlingsId: UUID,
        barnIdMap: Map<TidligereBarnId, NyttBarnId>,
    ): Map<UUID, Vilkår> =
        tidligereVilkår.values
            .associate { vilkår ->
                vilkår.id to vilkår.copy(
                    id = UUID.randomUUID(),
                    behandlingId = nyBehandlingsId,
                    sporbar = Sporbar(),
                    barnId = finnBarnId(vilkår.barnId, barnIdMap),
                    opphavsvilkår = vilkår.opprettOpphavsvilkår(),
                )
            }

    private fun opprettVilkårForNyeBarn(
        vilkårKopi: Map<UUID, Vilkår>,
        nyBehandling: Behandling,
        stønadstype: Stønadstype,
    ): List<Vilkår> {
        val alleBarn = barnService.finnBarnPåBehandling(nyBehandling.id)

        return alleBarn
            .filter { barn -> vilkårKopi.none { it.value.barnId == barn.id } }
            .map {
                OppdaterVilkår.lagVilkårForNyttBarn(
                    HovedregelMetadata(barn = alleBarn, behandling = nyBehandling),
                    it.behandlingId,
                    it.id,
                    stønadstype,
                )
            }
            .flatten()
    }

    private fun finnBarnId(barnId: UUID?, barnIdMap: Map<UUID, UUID>): UUID? =
        barnId?.let {
            barnIdMap[it]
                ?: error("Fant ikke barn=$it på gjeldende behandling med barnIdMapping=$barnIdMap")
        }

    fun erAlleVilkårOppfylt(behandlingId: UUID): Boolean {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val lagretVilkårsett = vilkårRepository.findByBehandlingId(behandlingId)
        return OppdaterVilkår.erAlleVilkårOppfylt(lagretVilkårsett, stønadstype)
    }

    fun hentOppfyltePassBarnVilkår(behandlingId: UUID): List<Vilkår> {
        return hentPassBarnVilkår(behandlingId)
            .filter { it.resultat == Vilkårsresultat.OPPFYLT }
    }

    fun hentPassBarnVilkår(behandlingId: UUID): List<Vilkår> {
        return vilkårRepository.findByBehandlingId(behandlingId)
            .filter { it.type == VilkårType.PASS_BARN }
    }

    companion object {

        fun byggBarnMapFraTidligereTilNyId(
            barnPåForrigeBehandling: List<BehandlingBarn>,
            barnPåGjeldendeBehandling: List<BehandlingBarn>,
        ): Map<UUID, BehandlingBarn> {
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
