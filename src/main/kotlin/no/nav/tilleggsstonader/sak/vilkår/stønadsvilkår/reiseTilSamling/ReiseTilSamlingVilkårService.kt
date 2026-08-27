package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
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
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeReiseTilSamling
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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.LagreVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReiseTilSamlingVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
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
        validerFeatureToggle()
        validerAktivitet(nyttVilkår, behandling)

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
        validerFeatureToggle()
        validerAktivitet(nyttVilkår, behandling)

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
            fakta = nyttVilkår.fakta,
        )
    }

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

    private fun validerFeatureToggle() {
        val kanBehandleReiseTilSamling = unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_TIL_SAMLING)

        feilHvis(!kanBehandleReiseTilSamling) {
            "TS-sak støtter foreløpig ikke behandling av saker som gjelder reise til samling"
        }
    }

    private fun validerAktivitet(
        nyttVilkår: LagreVilkårReiseTilSamling,
        behandling: Saksbehandling,
    ) {
        if (behandling.stønadstype != Stønadstype.REISE_TIL_SAMLING_TSR) return

        val aktivitetId =
            when (nyttVilkår.fakta.type) {
                TypeReiseTilSamling.PRIVAT_BIL -> (nyttVilkår.fakta as FaktaPrivatBil).aktivitetId
                TypeReiseTilSamling.OFFENTLIG_TRANSPORT -> (nyttVilkår.fakta as FaktaOffentligTransport).aktivitetId
                else -> return
            }

        validerAktivitetId(aktivitetId, nyttVilkår, behandling.id)
    }

    private fun validerAktivitetId(
        aktivitetId: VilkårperiodeGlobalId?,
        nyttVilkår: LagreVilkårReiseTilSamling,
        behandlingId: BehandlingId,
    ) {
        brukerfeilHvis(aktivitetId == null) {
            "Aktivitet må velges"
        }
        val aktivitet = vilkårperiodeService.hentAktivitet(aktivitetId, behandlingId)
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
}
