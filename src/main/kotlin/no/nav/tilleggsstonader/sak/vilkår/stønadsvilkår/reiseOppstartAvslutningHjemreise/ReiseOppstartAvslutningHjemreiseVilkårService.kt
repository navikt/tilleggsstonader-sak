package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseOppstartAvslutningHjemreiseRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReiseOppstartAvslutningHjemreiseVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val unleashService: UnleashService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårReiseOppstartAvslutningHjemreise> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .map { it.mapTilVilkårReiseOppstartAvslutningHjemreise() }
            .sortedBy { it.fom }

    @Transactional
    fun opprettNyttVilkår(
        nyttVilkår: LagreVilkårReiseOppstartAvslutningHjemreise,
        behandlingId: BehandlingId,
    ): VilkårReiseOppstartAvslutningHjemreise {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFeatureToggle()
        // TODO: valider at aktivitet er oppfylt hele perioden for TSR, tilsvarende ReiseTilSamlingVilkårService.validerAktivitet

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår)
        val lagretVilkår = vilkårRepository.insert(vilkår.mapTilVilkår())

        return lagretVilkår.mapTilVilkårReiseOppstartAvslutningHjemreise()
    }

    @Transactional
    fun oppdaterVilkår(
        nyttVilkår: LagreVilkårReiseOppstartAvslutningHjemreise,
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
    ): VilkårReiseOppstartAvslutningHjemreise {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFeatureToggle()
        // TODO: valider at aktivitet er oppfylt hele perioden for TSR, tilsvarende ReiseTilSamlingVilkårService.validerAktivitet

        val eksisterendeVilkår = vilkårRepository.findByIdOrThrow(vilkårId).mapTilVilkårReiseOppstartAvslutningHjemreise()

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår, eksisterendeVilkår)
        val lagretVilkår = vilkårRepository.update(vilkår.mapTilVilkår())

        return lagretVilkår.mapTilVilkårReiseOppstartAvslutningHjemreise()
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
        nyttVilkår: LagreVilkårReiseOppstartAvslutningHjemreise,
        eksisterendeVilkår: VilkårReiseOppstartAvslutningHjemreise? = null,
    ): VilkårReiseOppstartAvslutningHjemreise {
        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = ReiseOppstartAvslutningHjemreiseRegel(),
                svar = nyttVilkår.svar,
            )

        return VilkårReiseOppstartAvslutningHjemreise(
            behandlingId = behandlingId,
            id = eksisterendeVilkår?.id ?: VilkårId.random(),
            fom = nyttVilkår.fom,
            tom = nyttVilkår.tom,
            status = utledStatus(eksisterendeVilkår),
            delvilkårsett = delvilkårsett,
            resultat = RegelEvaluering.utledVilkårResultat(delvilkårsett),
            fakta = nyttVilkår.fakta,
        )
    }

    private fun utledStatus(eksisterendeVilkår: VilkårReiseOppstartAvslutningHjemreise?): VilkårStatus? =
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
}
