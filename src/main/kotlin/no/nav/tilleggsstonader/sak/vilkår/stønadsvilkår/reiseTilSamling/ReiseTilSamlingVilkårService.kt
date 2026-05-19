package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseTilSamlingRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingMapper.mapTilVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.LagreVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ReiseTilSamlingVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val unleashService: UnleashService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårReiseTilSamling> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .map { it.mapTilVilkårReiseTilSamling() }
            .sortedBy { it.fom }

    @Transactional
    fun opprettNyttVilkår(
        nyttVilkår: LagreVilkårReiseTilSamling,
        behandlingId: BehandlingId,
    ): VilkårReiseTilSamling {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerKanBehandleVilkåret()

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår)
        val lagretVilkår = vilkårRepository.insert(vilkår.mapTilVilkår())

        return lagretVilkår.mapTilVilkårReiseTilSamling()
    }

    @Transactional
    fun oppdaterVilkår(
        nyttVilkår: LagreVilkårReiseTilSamling,
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
    ): VilkårReiseTilSamling {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerKanBehandleVilkåret()

        val eksisterendeVilkår = vilkårRepository.findByIdOrThrow(vilkårId).mapTilVilkårReiseTilSamling()

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår, eksisterendeVilkår)
        val lagretVilkår = vilkårRepository.update(vilkår.mapTilVilkår())

        return lagretVilkår.mapTilVilkårReiseTilSamling()
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
        nyttVilkår: LagreVilkårReiseTilSamling,
        eksisterendeVilkår: VilkårReiseTilSamling? = null,
    ): VilkårReiseTilSamling {
        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = ReiseTilSamlingRegel(),
                svar = nyttVilkår.svar,
            )

        return VilkårReiseTilSamling(
            behandlingId = behandlingId,
            id = eksisterendeVilkår?.id ?: VilkårId.random(),
            fom = nyttVilkår.fom,
            tom = nyttVilkår.tom,
            status = utledStatus(eksisterendeVilkår),
            delvilkårsett = delvilkårsett,
            resultat = RegelEvaluering.utledVilkårResultat(delvilkårsett),
            fakta = nyttVilkår.fakta.fjern0Verdier(),
        )
    }

    private fun FaktaReiseTilSamling.fjern0Verdier(): FaktaReiseTilSamling =
        copy(
            utgifterOffentligTransport = utgifterOffentligTransport?.takeIf { it > 0 },
            reiseavstand = reiseavstand?.takeIf { it > BigDecimal.ZERO },
        )

    private fun utledStatus(eksisterendeVilkår: VilkårReiseTilSamling?): VilkårStatus? =
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

    private fun validerKanBehandleVilkåret() {
        val kanBehandleReiseTilSamling = unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_TIL_SAMLING)

        feilHvis(!kanBehandleReiseTilSamling) {
            "TS-sak støtter foreløpig ikke behandling av saker som gjelder reise til samling"
        }
    }
}
