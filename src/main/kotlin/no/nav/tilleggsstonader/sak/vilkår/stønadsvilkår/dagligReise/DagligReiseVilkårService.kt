package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.Periode
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DagligReiseVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårDagligReise> =
        vilkårRepository.findByBehandlingId(behandlingId).map { it.mapTilVilkårDagligReise() }

    @Transactional
    fun opprettNyttVilkår(
        nyttVilkår: LagreDagligReise,
        behandlingId: BehandlingId,
    ): VilkårDagligReise {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår)
        val lagretVilkår = vilkårRepository.insert(vilkår.mapTilVilkår())

        return lagretVilkår.mapTilVilkårDagligReise()
    }

    @Transactional
    fun oppdaterVilkår(
        nyttVilkår: LagreDagligReise,
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
    ): VilkårDagligReise {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)

        val eksisterendeVilkår = vilkårRepository.findByIdOrThrow(vilkårId).mapTilVilkårDagligReise()

        val vilkår = lagVilkårMedVurderingerOgResultat(behandlingId, nyttVilkår, eksisterendeVilkår)
        val lagretVilkår = vilkårRepository.update(vilkår.mapTilVilkår())

        return lagretVilkår.mapTilVilkårDagligReise()
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
        nyttVilkår: LagreDagligReise,
        eksisterendeVilkår: VilkårDagligReise? = null,
    ): VilkårDagligReise {
        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = DagligReiseRegel(),
                svar = nyttVilkår.svar,
            )

        return VilkårDagligReise(
            behandlingId = behandlingId,
            id = eksisterendeVilkår?.id ?: VilkårId.random(),
            fom = nyttVilkår.fom,
            tom = nyttVilkår.tom,
            status = utledStatus(eksisterendeVilkår),
            delvilkårsett = delvilkårsett,
            resultat = RegelEvaluering.utledVilkårResultat(delvilkårsett),
            fakta = nyttVilkår.fakta.fjern0Verdier(Periode(fom = nyttVilkår.fom, tom = nyttVilkår.tom)),
        )
    }

    private fun FaktaDagligReise?.fjern0Verdier(periode: Periode): FaktaDagligReise? {
        if (this == null) return null

        when (this) {
            is FaktaOffentligTransport -> return FaktaOffentligTransport(
                reisedagerPerUke = this.reisedagerPerUke,
                prisEnkelbillett = this.prisEnkelbillett?.takeIf { it > 0 },
                prisSyvdagersbillett = this.prisSyvdagersbillett?.takeIf { it > 0 },
                prisTrettidagersbillett = this.prisTrettidagersbillett?.takeIf { it > 0 },
                periode = periode,
            )

            is FaktaPrivatBil -> TODO()
        }
    }

    private fun utledStatus(eksisterendeVilkår: VilkårDagligReise?): VilkårStatus? =
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
}
