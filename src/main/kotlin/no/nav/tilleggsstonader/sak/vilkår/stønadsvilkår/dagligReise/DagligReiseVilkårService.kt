package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegel
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DagligReiseVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val unleashService: UnleashService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårDagligReise> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .map { it.mapTilVilkårDagligReise() }
            .sortedBy { it.fom }

    @Transactional
    fun opprettNyttVilkår(
        nyttVilkår: LagreDagligReise,
        behandlingId: BehandlingId,
    ): VilkårDagligReise {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerKanBehandleVilkåret(nyttVilkår, behandlingId)
        validerDelperiodeFomOgTomMotNyttVilkår(nyttVilkår)

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
        validerKanBehandleVilkåret(nyttVilkår, behandlingId)
        validerDelperiodeFomOgTomMotNyttVilkår(nyttVilkår)

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
            fakta = nyttVilkår.fakta.fjern0Verdier(Datoperiode(fom = nyttVilkår.fom, tom = nyttVilkår.tom)),
        )
    }

    private fun FaktaDagligReise.fjern0Verdier(periode: Datoperiode): FaktaDagligReise {
        when (this) {
            is FaktaOffentligTransport -> {
                return FaktaOffentligTransport(
                    reiseId = this.reiseId,
                    reisedagerPerUke = this.reisedagerPerUke,
                    prisEnkelbillett = this.prisEnkelbillett?.takeIf { it > 0 },
                    prisSyvdagersbillett = this.prisSyvdagersbillett?.takeIf { it > 0 },
                    prisTrettidagersbillett = this.prisTrettidagersbillett?.takeIf { it > 0 },
                    adresse = this.adresse,
                    periode = periode,
                    aktivitetId = this.aktivitetId,
                )
            }

            is FaktaPrivatBil -> {
                return this
            }

            is FaktaUbestemtType -> {
                return this
            }
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

    private fun validerKanBehandleVilkåret(
        nyttVilkår: LagreDagligReise,
        behandlingId: BehandlingId,
    ) {
        val gjelderPrivatBil = nyttVilkår.fakta.type == TypeDagligReise.PRIVAT_BIL
        val gjelderOffentligTransport = nyttVilkår.fakta.type == TypeDagligReise.OFFENTLIG_TRANSPORT
        val kanBehandlePrivatBil = unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL)
        val skalKnytteOffentligTransportTilAktivitet = unleashService.isEnabled(Toggle.KAN_KNYTTE_OFFENTLIG_TRANSPORT_TIL_AKTIVITET)

        feilHvis(gjelderPrivatBil && !kanBehandlePrivatBil) {
            "TS-sak støtter foreløpig ikke behandling av saker som gjelder privat bil"
        }

        if (gjelderPrivatBil) {
            validerAktivitetForPrivatBil(nyttVilkår, behandlingId)
        }

        if (gjelderOffentligTransport && skalKnytteOffentligTransportTilAktivitet) {
            validerAktivitetForOffentligTransport(nyttVilkår, behandlingId)
        }
    }

    private fun validerDelperiodeFomOgTomMotNyttVilkår(nyttVilkår: LagreDagligReise) {
        if (nyttVilkår.fakta is FaktaPrivatBil) {
            val fom = nyttVilkår.fom
            val tom = nyttVilkår.tom
            val delperiodeFom = nyttVilkår.fakta.faktaDelperioder.minOfOrNull { it.fom }
            val delperiodeTom = nyttVilkår.fakta.faktaDelperioder.maxOfOrNull { it.tom }

            brukerfeilHvisIkke(fom == delperiodeFom) {
                "Delperioden sin fom ${delperiodeFom?.norskFormat()} er ikke den samme som reiseperioden sin fom ${fom.norskFormat()}"
            }
            brukerfeilHvisIkke(tom == delperiodeTom) {
                "Delperioden sin tom ${delperiodeTom?.norskFormat()} er ikke den samme som reiseperioden sin tom ${tom.norskFormat()}"
            }
        }
    }

    private fun validerAktivitetForPrivatBil(
        nyttVilkår: LagreDagligReise,
        behandlingId: BehandlingId,
    ) {
        val fakta = nyttVilkår.fakta as FaktaPrivatBil
        val aktivitet = vilkårperiodeService.hentAktivitet(fakta.aktivitetId, behandlingId)
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

    private fun validerAktivitetForOffentligTransport(
        nyttVilkår: LagreDagligReise,
        behandlingId: BehandlingId,
    ) {
        val fakta = nyttVilkår.fakta as FaktaOffentligTransport
        brukerfeilHvis(fakta.aktivitetId == null) {
            "Aktivitet må velges for offentlig transport"
        }
        val aktivitet = vilkårperiodeService.hentAktivitet(fakta.aktivitetId, behandlingId)
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
