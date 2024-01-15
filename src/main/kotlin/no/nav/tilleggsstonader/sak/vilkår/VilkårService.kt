package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vilkår.EvalueringVilkårperiode.evaulerVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.AktivitetReelArbeidssøkerRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.AktivitetTiltakRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.AktivitetUtdanningRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeAAPFerdigAvklartRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeAAPRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeDagpengerRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeOmstillingsstønadRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeOvergangsstønadRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeUføretrygdRegel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårService(
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val vilkårRepository: VilkårRepository,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
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

    fun hentVilkårperioder(behandlingId: UUID): Vilkårperioder {
        val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId)

        return Vilkårperioder(
            målgrupper = finnPerioder<MålgruppeType>(vilkårsperioder),
            aktiviteter = finnPerioder<AktivitetType>(vilkårsperioder),
        )
    }

    private inline fun <reified T : VilkårperiodeType> finnPerioder(
        vilkårsperioder: List<Vilkårperiode>,
    ) = vilkårsperioder.filter { it.type is T }.map(Vilkårperiode::tilDto)

    @Transactional
    fun opprettVilkårperiode(behandlingId: UUID, opprettVilkårperiode: OpprettVilkårperiode): VilkårperiodeDto {
        feilHvis(behandlingErLåstForVidereRedigering(behandlingId)) {
            "Kan ikke opprette vilkår når behandling er låst for videre redigering"
        }

        val vilkårperiode = vilkårperiodeRepository.insert(
            Vilkårperiode(
                behandlingId = behandlingId,
                fom = opprettVilkårperiode.fom,
                tom = opprettVilkårperiode.tom,
                type = opprettVilkårperiode.type,
                detaljer = opprettVilkårperiode.detaljer,
                begrunnelse = opprettVilkårperiode.begrunnelse,
                resultat = evaulerVilkårperiode(opprettVilkårperiode.detaljer),
                kilde = KildeVilkårsperiode.MANUELL,
            ),
        )

        return vilkårperiode.tilDto()
    }

    fun slettVilkårperiode(behandlingId: UUID, id: UUID, slettVikårperiode: SlettVikårperiode): Vilkårperiode {
        val vilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(behandlingId, vilkårperiode.behandlingId)

        feilHvis(behandlingErLåstForVidereRedigering(vilkårperiode.behandlingId)) {
            "Kan ikke slette vilkårperiode når behandling er låst for videre redigering"
        }

        return vilkårperiodeRepository.update(
            vilkårperiode.copy(
                resultat = ResultatVilkårperiode.SLETTET,
                slettetKommentar = slettVikårperiode.kommentar,
            ),
        )
    }

    private fun vilkårsregelForVilkårsperiodeType(vilkårperiodeType: VilkårperiodeType) =
        when (vilkårperiodeType) {
            MålgruppeType.AAP -> MålgruppeAAPRegel()
            MålgruppeType.AAP_FERDIG_AVKLART -> MålgruppeAAPFerdigAvklartRegel()
            MålgruppeType.DAGPENGER -> MålgruppeDagpengerRegel()
            MålgruppeType.OMSTILLINGSSTØNAD -> MålgruppeOmstillingsstønadRegel()
            MålgruppeType.OVERGANGSSTØNAD -> MålgruppeOvergangsstønadRegel()
            MålgruppeType.UFØRETRYGD -> MålgruppeUføretrygdRegel()

            AktivitetType.TILTAK -> AktivitetTiltakRegel()
            AktivitetType.UTDANNING -> AktivitetUtdanningRegel()
            AktivitetType.REEL_ARBEIDSSØKER -> AktivitetReelArbeidssøkerRegel()
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
        val barn = barnService.finnBarnPåBehandling(behandlingId)
        val grunnlag = vilkårGrunnlagService.hentGrunnlag(behandlingId)
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
