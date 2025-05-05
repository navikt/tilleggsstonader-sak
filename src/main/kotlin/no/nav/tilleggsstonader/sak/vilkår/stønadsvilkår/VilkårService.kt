package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.barn.TidligereBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerSlettPeriodeRevurdering
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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.lagNyttVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.finnesVilkårTypeForStønadstype
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.hentVilkårsregel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårService(
    private val behandlingService: BehandlingService,
    private val vilkårRepository: VilkårRepository,
    private val barnService: BarnService,
) {
    @Transactional
    fun oppdaterVilkår(svarPåVilkårDto: SvarPåVilkårDto): Vilkår {
        val vilkår = vilkårRepository.findByIdOrThrow(svarPåVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, svarPåVilkårDto.behandlingId)

        val oppdatertVilkår = flettVilkårOgVurderResultat(vilkår, svarPåVilkårDto)
        validerEndrePeriodeRevurdering(behandling, vilkår, oppdatertVilkår)

        return vilkårRepository.update(oppdatertVilkår)
    }

    @Transactional
    fun opprettNyttVilkår(opprettVilkårDto: OpprettVilkårDto): Vilkår {
        val behandlingId = opprettVilkårDto.behandlingId

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerBehandlingOgVilkårType(behandlingId, opprettVilkårDto.vilkårType)

        val relevanteRegler = hentVilkårsregel(opprettVilkårDto.vilkårType)
        val metadata = hentHovedregelMetadata(behandling)
        validerBarnFinnesPåBehandling(metadata, opprettVilkårDto)

        val nyttVilkår =
            lagNyttVilkår(
                behandlingId = behandlingId,
                metadata = metadata,
                vilkårsregel = relevanteRegler,
                barnId = opprettVilkårDto.barnId,
            )
        val oppdatertVilkår = flettVilkårOgVurderResultat(nyttVilkår, opprettVilkårDto)
        validerNyPeriodeRevurdering(behandling, oppdatertVilkår)

        return vilkårRepository.insert(oppdatertVilkår)
    }

    private fun flettVilkårOgVurderResultat(
        vilkår: Vilkår,
        lagreVilkårDto: LagreVilkårDto,
    ): Vilkår {
        val vurderingsresultat =
            OppdaterVilkår.validerVilkårOgBeregnResultat(
                vilkår = vilkår,
                oppdatering = lagreVilkårDto,
            )
        val oppdatertVilkår =
            OppdaterVilkår.oppdaterVilkår(
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

        // TODO burde slettemarkeres hvis opprettet i tidligere behandling og ikke er fremtidig utgift?
        feilHvis(!vilkår.kanSlettes()) {
            "Kan ikke slette vilkår opprettet i tidligere behandling"
        }
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerSlettPeriodeRevurdering(behandling, vilkår)

        vilkårRepository.deleteById(vilkårId)
    }

    @Transactional
    fun settVilkårTilSkalIkkeVurderes(oppdaterVilkårDto: OppdaterVilkårDto): VilkårDto {
        val vilkår = vilkårRepository.findByIdOrThrow(oppdaterVilkårDto.id)
        val behandlingId = vilkår.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, oppdaterVilkårDto.behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)

        return oppdaterVilkårTilSkalIkkeVurderes(behandling, vilkår)
    }

    private fun oppdaterVilkårTilSkalIkkeVurderes(
        behandling: Saksbehandling,
        vilkår: Vilkår,
    ): VilkårDto {
        val metadata = hentHovedregelMetadata(behandling)
        val nyeDelvilkår =
            hentVilkårsregel(vilkår.type).initiereDelvilkår(
                metadata,
                Vilkårsresultat.SKAL_IKKE_VURDERES,
            )
        val delvilkårWrapper = DelvilkårWrapper(nyeDelvilkår)
        return vilkårRepository
            .update(
                vilkår.copy(
                    resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                    delvilkårwrapper = delvilkårWrapper,
                    opphavsvilkår = null,
                ),
            ).tilDto()
    }

    private fun hentHovedregelMetadata(behandling: Saksbehandling): HovedregelMetadata {
        val barn = barnService.finnBarnPåBehandling(behandling.id)
        return HovedregelMetadata(barn, behandling)
    }

    private fun validerBehandlingOgVilkårType(
        behandlingId: BehandlingId,
        vilkårType: VilkårType,
    ) {
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

    private fun validerBarnFinnesPåBehandling(
        metadata: HovedregelMetadata,
        opprettVilkårDto: OpprettVilkårDto,
    ) {
        if (opprettVilkårDto.barnId == null) return
        val barnIderPåBehandling = metadata.barn.map { it.id }.toSet()
        feilHvisIkke(barnIderPåBehandling.contains(opprettVilkårDto.barnId)) {
            "Finner ikke barn på behandling"
        }
    }

    private fun validerLåstForVidereRedigering(behandling: Saksbehandling) {
        behandling.status.validerKanBehandlingRedigeres()
    }

    /**
     * Tilgangskontroll sjekker att man har tilgang til behandlingId som blir sendt inn, men det er mulig å sende inn
     * en annen behandlingId enn den som er på vilkåret
     */
    private fun validerBehandlingIdErLikIRequestOgIVilkåret(
        behandlingId: BehandlingId,
        requestBehandlingId: BehandlingId,
    ) {
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

    fun hentVilkår(behandlingId: BehandlingId): List<Vilkår> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .sortedWith(compareBy({ it.fom }, { it.tom }))

    /**
     * Når en revurdering opprettes skal den kopiere de tidligere vilkårene for samme stønad.
     */
    fun kopierVilkårsettTilNyBehandling(
        forrigeIverksatteBehandlingId: BehandlingId,
        nyBehandling: Behandling,
        barnIdMap: Map<TidligereBarnId, NyttBarnId>,
    ) {
        val tidligereVurderinger =
            vilkårRepository.findByBehandlingId(forrigeIverksatteBehandlingId).associateBy { it.id }

        val kopiAvVurderinger =
            lagKopiAvTidligereVurderinger(
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
        tidligereVilkår.values.filter { it.skalKopieres() }.map { vilkår ->
            val barnIdINyBehandling = finnBarnId(vilkår.barnId, barnIdMap)
            vilkår.kopierTilBehandling(nyBehandlingsId, barnIdINyBehandling)
        }

    private fun finnBarnId(
        barnId: BarnId?,
        barnIdMap: Map<BarnId, BarnId>,
    ): BarnId? =
        barnId?.let {
            barnIdMap[it]
                ?: error("Fant ikke barn=$it på gjeldende behandling med barnIdMapping=$barnIdMap")
        }

    fun hentOppfyltePassBarnVilkår(behandlingId: BehandlingId): List<Vilkår> =
        hentPassBarnVilkår(behandlingId)
            .filter { it.resultat == Vilkårsresultat.OPPFYLT }

    fun hentPassBarnVilkår(behandlingId: BehandlingId): List<Vilkår> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .filter { it.type == VilkårType.PASS_BARN }

    fun hentOppfylteBoutgiftVilkår(behandlingId: BehandlingId): List<Vilkår> =
        hentBoutgiftVilkår(behandlingId)
            .filter { it.resultat == Vilkårsresultat.OPPFYLT }

    fun hentBoutgiftVilkår(behandlingId: BehandlingId): List<Vilkår> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .also { it.kastFeilHvisTypeIkkeGjelderBoutgifter() }
}

private fun List<Vilkår>.kastFeilHvisTypeIkkeGjelderBoutgifter() {
    feilHvis(
        none {
            it.type == VilkårType.UTGIFTER_OVERNATTING ||
                it.type == VilkårType.LØPENDE_UTGIFTER_EN_BOLIG ||
                it.type == VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER
        },
    ) {
        "Forventet at vilkåret var av type boutgift, fikk i stedet ${map { it.type }}"
    }
}
