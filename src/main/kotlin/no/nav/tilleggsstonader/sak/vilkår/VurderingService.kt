package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsvurdering
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårsvurderingRepository
import no.nav.tilleggsstonader.sak.vilkår.dto.Fellesgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkårsvurderinger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingService(
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val barnService: BarnService,
    private val vilkårGrunnlagService: VilkårGrunnlagService,
    // private val grunnlagsdataService: GrunnlagsdataService,
    private val fagsakService: FagsakService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun hentEllerOpprettVurderinger(behandlingId: UUID): VilkårDto {
        val (grunnlag, metadata) = hentGrunnlagOgMetadata(behandlingId)
        val vurderinger = hentEllerOpprettVurderinger(behandlingId, metadata)
        return VilkårDto(vurderinger = vurderinger, grunnlag = grunnlag)
    }

    @Transactional
    fun hentOpprettEllerOppdaterVurderinger(behandlingId: UUID): VilkårDto {
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
        return hentEllerOpprettVurderinger(behandlingId)
    }

    fun hentAlleVurderinger(behandlingId: UUID): List<VilkårsvurderingDto> {
        val vurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        feilHvis(vurderinger.isEmpty()) {
            "Mangler vurderinger for behandling=$behandlingId"
        }
        return vurderinger.map { it.tilDto() }
    }

    @Transactional
    fun oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandlingId: UUID): VilkårDto {
        // grunnlagsdataService.oppdaterOgHentNyGrunnlagsdata(behandlingId)
        return hentEllerOpprettVurderinger(behandlingId)
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

    fun hentGrunnlagOgMetadata(behandlingId: UUID): Pair<VilkårGrunnlagDto, HovedregelMetadata> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        /*val søknad = søknadService.hentSøknadsgrunnlag(behandlingId)
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val barn = barnService.finnBarnPåBehandling(behandlingId)
        val grunnlag = vilkårGrunnlagService.hentGrunnlag(behandlingId, søknad, personIdent, barn)
        val søktOmBarnetilsyn =
            grunnlag.barnMedSamvær.filter { it.barnepass?.skalHaBarnepass == true }.map { it.barnId }

        val metadata = HovedregelMetadata(
            sivilstandstype = grunnlag.sivilstand.registergrunnlag.type,
            sivilstandSøknad = søknad?.sivilstand,
            barn = barn,
            søktOmBarnetilsyn = søktOmBarnetilsyn,
            langAvstandTilSøker = grunnlag.barnMedSamvær.map { it.mapTilBarnForelderLangAvstandTilSøker() },
            vilkårgrunnlagDto = grunnlag,
            behandling = behandling,
        )
         */
        return Pair(VilkårGrunnlagDto(Fellesgrunnlag("navn")), HovedregelMetadata(emptyList(), behandling))
    }

    private fun hentEllerOpprettVurderinger(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
    ): List<VilkårsvurderingDto> {
        return hentEllerOpprettVurderingerForVilkår(behandlingId, metadata).map(Vilkårsvurdering::tilDto)
    }

    private fun hentEllerOpprettVurderingerForVilkår(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
    ): List<Vilkårsvurdering> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        return when {
            behandlingErLåstForVidereRedigering(behandlingId) -> lagredeVilkårsvurderinger
            lagredeVilkårsvurderinger.isEmpty() -> lagreNyeVilkårsvurderinger(behandlingId, metadata)
            else -> lagredeVilkårsvurderinger
        }
    }

    private fun lagreNyeVilkårsvurderinger(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
    ): List<Vilkårsvurdering> {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val nyeVilkårsvurderinger: List<Vilkårsvurdering> = opprettNyeVilkårsvurderinger(
            behandlingId = behandlingId,
            metadata = metadata,
            stønadstype = stønadstype,
        )
        return vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
        behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    /*private fun finnEndringerIGrunnlagsdata(behandlingId: UUID): List<GrunnlagsdataEndring> {
        val oppdaterteGrunnlagsdata = grunnlagsdataService.hentFraRegister(behandlingId)
        val eksisterendeGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        return oppdaterteGrunnlagsdata.endringerMellom(eksisterendeGrunnlagsdata)
    }*/

    /**
     * Når en revurdering opprettes skal den kopiere de tidligere vilkårsvurderingene med lik verdi for endretTid.
     * Endret tid blir satt av Sporbar() som alltid vil sette endretTid til nåværende tispunkt, noe som blir feil.
     * For å omgå dette problemet lagres først de kopierte vilkårsvurderingene til databasen. Til slutt
     * vil oppdaterEndretTid() manuelt overskrive verdiene for endretTid til korrekte verdier.
     */
    fun kopierVurderingerTilNyBehandling(
        eksisterendeBehandlingId: UUID,
        nyBehandlingsId: UUID,
        metadata: HovedregelMetadata,
        stønadstype: Stønadstype,
    ) {
        val tidligereVurderinger =
            vilkårsvurderingRepository.findByBehandlingId(eksisterendeBehandlingId).associateBy { it.id }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(eksisterendeBehandlingId)
        val barnIdMap = byggBarnMapFraTidligereTilNyId(barnPåForrigeBehandling, metadata.barn)
        validerAtVurderingerKanKopieres(tidligereVurderinger, eksisterendeBehandlingId)

        val kopiAvVurderinger: Map<UUID, Vilkårsvurdering> = lagKopiAvTidligereVurderinger(
            tidligereVurderinger,
            metadata.barn,
            nyBehandlingsId,
            barnIdMap,
        )

        val nyeBarnVurderinger = opprettVurderingerForNyeBarn(kopiAvVurderinger, metadata, stønadstype)

        vilkårsvurderingRepository.insertAll(kopiAvVurderinger.values.toList() + nyeBarnVurderinger)
        tilbakestillEndretTidForKopierteVurderinger(kopiAvVurderinger, tidligereVurderinger)
    }

    private fun validerAtVurderingerKanKopieres(
        tidligereVurderinger: Map<UUID, Vilkårsvurdering>,
        eksisterendeBehandlingId: UUID,
    ) {
        if (tidligereVurderinger.isEmpty()) {
            val melding = "Tidligere behandling=$eksisterendeBehandlingId har ikke noen vilkår"
            throw Feil(melding, melding)
        }
    }

    private fun lagKopiAvTidligereVurderinger(
        tidligereVurderinger: Map<UUID, Vilkårsvurdering>,
        barnPåGjeldendeBehandling: List<BehandlingBarn>,
        nyBehandlingsId: UUID,
        barnIdMap: Map<UUID, BehandlingBarn>,
    ): Map<UUID, Vilkårsvurdering> =
        tidligereVurderinger.values
            .filter { skalKopiereVurdering(it, barnPåGjeldendeBehandling.isNotEmpty()) }
            .associate { vurdering ->
                vurdering.id to vurdering.copy(
                    id = UUID.randomUUID(),
                    behandlingId = nyBehandlingsId,
                    sporbar = Sporbar(),
                    barnId = finnBarnId(vurdering.barnId, barnIdMap),
                    opphavsvilkår = vurdering.opprettOpphavsvilkår(),
                )
            }

    private fun opprettVurderingerForNyeBarn(
        vurderingerKopi: Map<UUID, Vilkårsvurdering>,
        metadata: HovedregelMetadata,
        stønadstype: Stønadstype,
    ) =
        metadata.barn
            .filter { barn -> vurderingerKopi.none { it.value.barnId == barn.id } }
            .map { OppdaterVilkår.lagVilkårsvurderingForNyttBarn(metadata, it.behandlingId, it.id, stønadstype) }
            .flatten()

    private fun tilbakestillEndretTidForKopierteVurderinger(
        vurderinger: Map<UUID, Vilkårsvurdering>,
        tidligereVurderinger: Map<UUID, Vilkårsvurdering>,
    ) {
        vurderinger.forEach { (forrigeId, vurdering) ->
            vilkårsvurderingRepository.oppdaterEndretTid(
                vurdering.id,
                tidligereVurderinger.getValue(forrigeId).sporbar.endret.endretTid,
            )
        }
    }

    private fun finnBarnId(barnId: UUID?, barnIdMap: Map<UUID, BehandlingBarn>): UUID? {
        return barnId?.let {
            val barnIdMapping = barnIdMap.map { it.key to it.value.id }.toMap()
            barnIdMap[it]?.id ?: error("Fant ikke barn=$it på gjeldende behandling med barnIdMapping=$barnIdMapping")
        }
    }

    private fun skalKopiereVurdering(
        it: Vilkårsvurdering,
        harNyeBarnForVurdering: Boolean,
    ) =
        if (it.type.gjelderFlereBarn() && it.barnId == null) {
            !harNyeBarnForVurdering
        } else {
            true
        }

    fun erAlleVilkårOppfylt(behandlingId: UUID): Boolean {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val lagredeVilkårsvurderinger: List<Vilkårsvurdering> =
            vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        return OppdaterVilkår.erAlleVilkårsvurderingerOppfylt(lagredeVilkårsvurderinger, stønadstype)
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
