package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.feil.feilHvisIkke
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.hentVilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.AktivitetMedReiser
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReiseOppstartAvslutningHjemreiseVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val unleashService: UnleashService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårROAH> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .medResultatFraParent()
            .map { it.mapTilVilkårROAH() }
            .sortedBy { it.fom }

    fun hentVilkårGruppertPåAktivitet(behandlingId: BehandlingId): List<AktivitetMedReiser> {
        val aktiviteter = vilkårperiodeService.hentVilkårperioder(behandlingId).aktiviteter
        val vilkår = hentVilkårForBehandling(behandlingId)
        val reiserPerAktivitet =
            vilkår.filterIsInstance<VilkårReiseOppstartAvslutningHjemreise>().groupBy { it.aktivitetId }
        val paraplyvilkårPerAktivitet = vilkår.filterIsInstance<VilkårMidlertidigBosted>().groupBy { it.aktivitetId }

        return aktiviteter
            .sortedBy { it.fom }
            .map { aktivitet ->
                AktivitetMedReiser(
                    aktivitetId = aktivitet.globalId,
                    aktivitetType = aktivitet.type as AktivitetType,
                    tiltaksvariant = aktivitet.tiltaksvariant,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                    reiser = reiserPerAktivitet[aktivitet.globalId].orEmpty(),
                    paraplyvilkår = paraplyvilkårPerAktivitet[aktivitet.globalId].orEmpty(),
                )
            }
    }

    @Transactional
    fun opprettNyttVilkår(
        nyttVilkår: LagreVilkårROAH,
        behandlingId: BehandlingId,
    ): VilkårROAH {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFeatureToggle()
        validerAktivitet(nyttVilkår, behandlingId)
        validerTilhørendeParaplyvilkår(nyttVilkår, behandlingId, vilkårId = null)

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår)
        val lagretVilkår = vilkårRepository.insert(vilkår.mapTilVilkår())
        val parentVilkår = finnParent(lagretVilkår)
        return lagretVilkår.medResultatFraParent(parentVilkår).mapTilVilkårROAH()
    }

    @Transactional
    fun oppdaterVilkår(
        nyttVilkår: LagreVilkårROAH,
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
    ): VilkårROAH {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFeatureToggle()
        validerAktivitet(nyttVilkår, behandlingId)
        validerTilhørendeParaplyvilkår(nyttVilkår, behandlingId, vilkårId)

        val eksisterendeVilkår = hentVilkårForBehandling(vilkårId, behandlingId)
        validerKanOppdatereVilkår(eksisterendeVilkår, nyttVilkår)

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår, eksisterendeVilkår)
        val lagretVilkår = vilkårRepository.update(vilkår.mapTilVilkår())
        val parentVilkår = finnParent(lagretVilkår)
        return lagretVilkår.medResultatFraParent(parentVilkår).mapTilVilkårROAH()
    }

    @Transactional
    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        slettetKommentar: String?,
    ): SlettetVilkårResultat =
        vilkårService.slettVilkår(
            SlettVilkårRequest(
                id = vilkårId,
                behandlingId = behandlingId,
                kommentar = slettetKommentar,
            ),
        )

    private fun lagVilkårMedVurderingerOgResultat(
        behandlingId: BehandlingId,
        nyttVilkår: LagreVilkårROAH,
        eksisterendeVilkår: Vilkår? = null,
    ): VilkårROAH {
        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = hentVilkårsregel(nyttVilkår.vilkårType),
                svar = nyttVilkår.svar,
            )
        return when (nyttVilkår) {
            is LagreVilkårReiseOppstartAvslutningHjemreise ->
                VilkårReiseOppstartAvslutningHjemreise(
                    behandlingId = behandlingId,
                    id = eksisterendeVilkår?.id ?: VilkårId.random(),
                    globalId = eksisterendeVilkår?.globalId ?: VilkårGlobalId.random(),
                    fom = nyttVilkår.fom,
                    tom = nyttVilkår.tom,
                    aktivitetId = nyttVilkår.aktivitetId,
                    reiseId = nyttVilkår.reiseId,
                    typeReiseformål = nyttVilkår.typeReiseformål,
                    status = utledStatus(eksisterendeVilkår),
                    delvilkårsett = delvilkårsett,
                    resultat = RegelEvaluering.utledVilkårResultat(delvilkårsett),
                    fakta = nyttVilkår.fakta,
                    parentVilkår = nyttVilkår.parentVilkår,
                )

            is LagreVilkårMidlertidigBosted ->
                VilkårMidlertidigBosted(
                    behandlingId = behandlingId,
                    id = eksisterendeVilkår?.id ?: VilkårId.random(),
                    globalId = eksisterendeVilkår?.globalId ?: VilkårGlobalId.random(),
                    fom = nyttVilkår.fom,
                    tom = nyttVilkår.tom,
                    aktivitetId = nyttVilkår.aktivitetId,
                    status = utledStatus(eksisterendeVilkår),
                    delvilkårsett = delvilkårsett,
                    resultat = RegelEvaluering.utledVilkårResultat(delvilkårsett),
                    fraAdresse = nyttVilkår.fraAdresse,
                    tilAdresse = nyttVilkår.tilAdresse,
                )
        }
    }

    private fun finnParent(vilkår: Vilkår): Vilkår? =
        vilkår.parentVilkår?.let { parentId ->
            vilkårRepository.findByBehandlingId(vilkår.behandlingId).singleOrNull { it.globalId == parentId }
        }

    private fun hentVilkårForBehandling(
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): Vilkår {
        val vilkår = vilkårRepository.findByIdOrThrow(vilkårId)
        feilHvis(vilkår.behandlingId != behandlingId) { "Vilkåret tilhører ikke behandlingen" }
        return vilkår
    }

    /**
     * Endepunktet kan bare oppdatere vilkår for reise oppstart, avslutning og hjemreise, ikke andre vilkårstyper.
     * En reise kan heller ikke bli et paraplyvilkår, eller omvendt, ved oppdatering.
     */
    private fun validerKanOppdatereVilkår(
        eksisterendeVilkår: Vilkår,
        nyttVilkår: LagreVilkårROAH,
    ) {
        feilHvis(eksisterendeVilkår.type !in ROAH_VILKÅRSTYPER) {
            "Vilkåret er ikke et vilkår for reise oppstart, avslutning og hjemreise"
        }
        val erEksisterendeVilkårParaplyvilkår = eksisterendeVilkår.type == VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE
        val erNyttVilkårParaplyvilkår = nyttVilkår is LagreVilkårMidlertidigBosted
        feilHvis(erEksisterendeVilkårParaplyvilkår != erNyttVilkårParaplyvilkår) {
            "Kan ikke endre mellom reise og paraplyvilkår"
        }
    }

    /**
     * En reise kan høre til et paraplyvilkår (midlertidig bosted), referert med paraplyvilkårets globalId.
     * globalId er ikke unik på tvers av behandlinger, så paraplyvilkåret slås alltid opp i samme behandling.
     * [vilkårId] er null når vilkåret opprettes. Feilmeldingene inneholder ingen verdier fra vilkåret.
     */
    private fun validerTilhørendeParaplyvilkår(
        nyttVilkår: LagreVilkårROAH,
        behandlingId: BehandlingId,
        vilkårId: VilkårId?,
    ) {
        val parentVilkårGlobalId = (nyttVilkår as? LagreVilkårReiseOppstartAvslutningHjemreise)?.parentVilkår ?: return

        val parentVilkår =
            vilkårRepository
                .findByBehandlingId(behandlingId)
                .singleOrNull { it.globalId == parentVilkårGlobalId }
        feilHvis(parentVilkår == null) { "Paraplyvilkåret finnes ikke i behandlingen" }
        feilHvis(parentVilkår.id == vilkårId) { "Et vilkår kan ikke referere til seg selv" }
        feilHvis(parentVilkår.status == VilkårStatus.SLETTET) { "Paraplyvilkåret er slettet" }
        feilHvis(parentVilkår.type != VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE) {
            "Referansen peker ikke på et paraplyvilkår"
        }

        val parentAktivitetId = (parentVilkår.fakta as? FaktaROAH)?.aktivitetId
        feilHvis(parentAktivitetId != nyttVilkår.aktivitetId) {
            "Reisen må høre til samme aktivitet som paraplyvilkåret"
        }
    }

    private fun utledStatus(eksisterendeVilkår: Vilkår?): VilkårStatus? =
        when {
            eksisterendeVilkår == null -> VilkårStatus.NY
            eksisterendeVilkår.status == VilkårStatus.UENDRET -> VilkårStatus.ENDRET
            else -> eksisterendeVilkår.status
        }

    private fun validerBehandling(behandling: Saksbehandling) {
        validerErIVilkårSteg(behandling)
        validerErRedigerbar(behandling)
    }

    private fun validerErIVilkårSteg(behandling: Saksbehandling) {
        feilHvisIkke(behandling.steg == StegType.VILKÅR) {
            "Kan ikke oppdatere vilkår når behandling er på steg=${behandling.steg}."
        }
    }

    private fun validerErRedigerbar(behandling: Saksbehandling) {
        behandling.status.validerKanBehandlingRedigeres()
    }

    private fun validerFeatureToggle() {
        val kanBehandleReiseOppstartAvslutningHjemreise =
            unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_OPPSTART_AVSLUTNING_HJEMREISE)

        feilHvis(!kanBehandleReiseOppstartAvslutningHjemreise) {
            "TS-sak støtter foreløpig ikke behandling av saker som gjelder reise oppstart, avslutning eller hjemreise"
        }
    }

    /**
     * Aktivitet valideres likt for TSO og TSR (i motsetning til reise til samling, der dette kun valideres for TSR).
     */
    private fun validerAktivitet(
        nyttVilkår: LagreVilkårROAH,
        behandlingId: BehandlingId,
    ) {
        val aktivitet = vilkårperiodeService.hentAktivitet(nyttVilkår.aktivitetId, behandlingId)

        brukerfeilHvis(aktivitet == null) {
            "Aktiviteten finnes ikke"
        }
        brukerfeilHvis(aktivitet.resultat != ResultatVilkårperiode.OPPFYLT) {
            "Aktiviteten er ikke oppfylt"
        }
        brukerfeilHvisIkke(aktivitet.inneholder(nyttVilkår)) {
            "Aktiviteten er ikke oppfylt hele vilkårperioden"
        }
    }

    private companion object {
        val ROAH_VILKÅRSTYPER =
            setOf(
                VilkårType.ROAH_OPPSTART_AVSLUTNING,
                VilkårType.ROAH_HJEMREISE,
                VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
            )
    }
}
