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
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.dto.Fellesgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkår
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårService(
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val vilkårRepository: VilkårRepository,
    private val barnService: BarnService,
    private val vilkårGrunnlagService: VilkårGrunnlagService,
    // private val grunnlagsdataService: GrunnlagsdataService,
    private val fagsakService: FagsakService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

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

    private fun hentEllerOpprettVilkår(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
    ): List<Vilkår> {
        val lagretVilkårsett = vilkårRepository.findByBehandlingId(behandlingId)

        return when {
            behandlingErLåstForVidereRedigering(behandlingId) -> lagretVilkårsett
            lagretVilkårsett.isEmpty() -> lagNyttVilkårsett(behandlingId, metadata)
            else -> lagretVilkårsett
        }
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

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
        behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    /*private fun finnEndringerIGrunnlagsdata(behandlingId: UUID): List<GrunnlagsdataEndring> {
        val oppdaterteGrunnlagsdata = grunnlagsdataService.hentFraRegister(behandlingId)
        val eksisterendeGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        return oppdaterteGrunnlagsdata.endringerMellom(eksisterendeGrunnlagsdata)
    }*/

    /**
     * Når en revurdering opprettes skal den kopiere de tidligere vilkåren med lik verdi for endretTid.
     * Endret tid blir satt av Sporbar() som alltid vil sette endretTid til nåværende tispunkt, noe som blir feil.
     * For å omgå dette problemet lagres først de kopierte vilkårsvurderingene til databasen. Til slutt
     * vil oppdaterEndretTid() manuelt overskrive verdiene for endretTid til korrekte verdier.
     */
    fun kopierVilkårsettTilNyBehandling(
        eksisterendeBehandlingId: UUID,
        nyBehandlingsId: UUID,
        metadata: HovedregelMetadata,
        stønadstype: Stønadstype,
    ) {
        val tidligereVurderinger =
            vilkårRepository.findByBehandlingId(eksisterendeBehandlingId).associateBy { it.id }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(eksisterendeBehandlingId)
        val barnIdMap = byggBarnMapFraTidligereTilNyId(barnPåForrigeBehandling, metadata.barn)
        validerAtVurderingerKanKopieres(tidligereVurderinger, eksisterendeBehandlingId)

        val kopiAvVurderinger: Map<UUID, Vilkår> = lagKopiAvTidligereVurderinger(
            tidligereVurderinger,
            metadata.barn,
            nyBehandlingsId,
            barnIdMap,
        )

        val nyeBarnVurderinger = opprettVilkårForNyeBarn(kopiAvVurderinger, metadata, stønadstype)

        vilkårRepository.insertAll(kopiAvVurderinger.values.toList() + nyeBarnVurderinger)
        tilbakestillEndretTidForKopierteVurderinger(kopiAvVurderinger, tidligereVurderinger) // TODO remove
    }

    private fun validerAtVurderingerKanKopieres(
        tidligereVurderinger: Map<UUID, Vilkår>,
        eksisterendeBehandlingId: UUID,
    ) {
        if (tidligereVurderinger.isEmpty()) {
            val melding = "Tidligere behandling=$eksisterendeBehandlingId har ikke noen vilkår"
            throw Feil(melding, melding)
        }
    }

    private fun lagKopiAvTidligereVurderinger(
        tidligereVilkår: Map<UUID, Vilkår>,
        barnPåGjeldendeBehandling: List<BehandlingBarn>,
        nyBehandlingsId: UUID,
        barnIdMap: Map<UUID, BehandlingBarn>,
    ): Map<UUID, Vilkår> =
        tidligereVilkår.values
            .filter { skalKopiereVilkår(it, barnPåGjeldendeBehandling.isNotEmpty()) }
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
        metadata: HovedregelMetadata,
        stønadstype: Stønadstype,
    ) =
        metadata.barn
            .filter { barn -> vilkårKopi.none { it.value.barnId == barn.id } }
            .map { OppdaterVilkår.lagVilkårForNyttBarn(metadata, it.behandlingId, it.id, stønadstype) }
            .flatten()

    private fun tilbakestillEndretTidForKopierteVurderinger(
        vilkår: Map<UUID, Vilkår>,
        tidligereVurderinger: Map<UUID, Vilkår>,
    ) {
        vilkår.forEach { (forrigeId, vurdering) ->
            vilkårRepository.oppdaterEndretTid(
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

    private fun skalKopiereVilkår(
        it: Vilkår,
        harNyeBarnForVilkår: Boolean,
    ) =
        if (it.type.gjelderFlereBarn() && it.barnId == null) {
            !harNyeBarnForVilkår
        } else {
            true
        }

    fun erAlleVilkårOppfylt(behandlingId: UUID): Boolean {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val lagretVilkårsett = vilkårRepository.findByBehandlingId(behandlingId)
        return OppdaterVilkår.erAlleVilkårOppfylt(lagretVilkårsett, stønadstype)
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
