package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårsvurderingJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OverordnetValgJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvaralternativJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.Vilkårsvurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkårsreglerForStønad
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
    private val behandlingFaktaService: BehandlingFaktaService,
    // private val grunnlagsdataService: GrunnlagsdataService,
    private val fagsakService: FagsakService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun hentEllerOpprettVilkårsvurdering(behandlingId: UUID): Vilkårsvurdering {
        val (grunnlag, metadata) = hentGrunnlagOgMetadata(behandlingId)
        val vurderinger = hentEllerOpprettVilkår(behandlingId, metadata).map(Vilkår::tilDto)
        return Vilkårsvurdering(vilkårsett = vurderinger, grunnlag = grunnlag)
    }

    @Transactional
    fun hentOpprettEllerOppdaterVilkårsvurdering(behandlingId: UUID): Vilkårsvurdering {
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

    data class VilkårsvurderingerJson(
        val vilkårsett: List<VilkårJson>,
        val grunnlag: BehandlingFaktaDto,
    )

    fun hentVilkårsett(behandlingId: UUID): List<VilkårDto> {
        val vilkårsett = vilkårRepository.findByBehandlingId(behandlingId)
        feilHvis(vilkårsett.isEmpty()) { "Mangler vilkår for behandling=$behandlingId" }
        return vilkårsett.map { it.tilDto() }
    }

    @Transactional
    fun oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandlingId: UUID): Vilkårsvurdering {
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
     * Når en revurdering opprettes skal den kopiere de tidligere vilkårene for samme stønad.
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

fun VilkårDto.tilJson(): VilkårJson {
    val vurderinger: List<VurderingDto> = this.delvilkårsett.flatMap { it.vurderinger }

    return VilkårJson(
        id = this.id,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        vilkårType = this.vilkårType,
        barnId = this.barnId,
        endretAv = this.endretAv,
        endretTid = this.endretTid,
        vurdering = vurderinger.tilJson(),
        opphavsvilkår = this.opphavsvilkår,
    )
}

private fun List<VurderingDto>.tilJson(): VilkårsvurderingJson {
    val vilkårsvurdering = mutableMapOf<RegelId, DelvilkårsvurderingJson>()
    val stønadsregler = vilkårsreglerPassBarn()
    for ((regel, regelSteg) in stønadsregler) {
        val vurderingDto = this.find { it.regelId == regel }

        vilkårsvurdering[regel] = delvilkårsvurderingMapper(regel, vurderingDto, regelSteg.svarMapping)
    }
    return vilkårsvurdering
}

fun delvilkårsvurderingMapper(
    gjeldendeRegel: RegelId,
    vurdering: VurderingDto?,
    svarMapping: Map<SvarId, SvarRegel>,
): DelvilkårsvurderingJson {
    val svaralternativer = svarMapping.entries.associate {
        it.key to SvaralternativJson(it.value.begrunnelseType)
    }

    data class Regelavhengighet(
        val denneRegelen: RegelId,
        val erAvhengigAvDenneRegelen: RegelId,
        val ogDetteSvaret: SvarId,
    )

    val relaterteOverordnedeValg = vilkårsreglerPassBarn().values.flatMap { regelSteg ->
        regelSteg.svarMapping.map {
            Regelavhengighet(
                denneRegelen = it.value.regelId,
                erAvhengigAvDenneRegelen = regelSteg.regelId,
                ogDetteSvaret = it.key,
            )
        }
    }

    val følgerFraOverordnetValg =
        relaterteOverordnedeValg.filter { it.denneRegelen == gjeldendeRegel }
            .map { OverordnetValgJson(it.erAvhengigAvDenneRegelen, it.ogDetteSvaret) }.firstOrNull()

    return DelvilkårsvurderingJson(
        følgerFraOverordnetValg = følgerFraOverordnetValg,
        svar = vurdering?.svar,
        begrunnelse = vurdering?.begrunnelse,
        svaralternativer = svaralternativer,
    )
}

fun vilkårsreglerPassBarn() = vilkårsreglerForStønad(Stønadstype.BARNETILSYN).map { it.regler }.first()

fun hovedreglerPassBarn() = vilkårsreglerForStønad(Stønadstype.BARNETILSYN).map { it.hovedregler }.first()
