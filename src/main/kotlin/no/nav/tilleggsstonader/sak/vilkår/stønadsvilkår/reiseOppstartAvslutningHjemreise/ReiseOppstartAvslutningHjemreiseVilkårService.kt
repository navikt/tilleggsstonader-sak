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
import no.nav.tilleggsstonader.sak.infrastruktur.database.AdvisoryLockService
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHMidlertidigBostedPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah.MidlertidigBostedPeriodeRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah.ROAHRegelUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.AktivitetMedReiser
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaMidlertidigBostedPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.ReiseFaktaROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.tilVilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ReiseOppstartAvslutningHjemreiseVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val unleashService: UnleashService,
    private val advisoryLockService: AdvisoryLockService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårReiseOppstartAvslutningHjemreise> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .filter { it.type.erROAHReise() }
            .map { it.mapTilVilkårReiseOppstartAvslutningHjemreise() }
            .sortedBy { it.fom }

    fun hentVilkårGruppertPåAktivitet(behandlingId: BehandlingId): List<AktivitetMedReiser> {
        val aktiviteter = vilkårperiodeService.hentVilkårperioder(behandlingId).aktiviteter
        val reiserPerAktivitet = hentVilkårForBehandling(behandlingId).groupBy { it.aktivitetId }
        val perioderPerAktivitet =
            vilkårRepository
                .findByBehandlingId(behandlingId)
                .filter { it.type == VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE }
                .sortedWith(compareBy({ it.fom }, { it.tom }))
                .groupBy { (it.fakta as FaktaROAHMidlertidigBostedPeriode).aktivitetId }

        // TODO: Bør markere reiser som tilhører en slettet aktivitet
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
                    midlertidigeBostedsperioder = perioderPerAktivitet[aktivitet.globalId].orEmpty(),
                )
            }
    }

    @Transactional
    fun opprettNyttVilkår(
        nyttVilkår: LagreVilkårReiseOppstartAvslutningHjemreise,
        behandlingId: BehandlingId,
    ): Vilkår =
        advisoryLockService.lockForTransaction("roah-midlertidig-bosted:$behandlingId") {
            validerLagring(behandlingId, nyttVilkår)
            val vilkår =
                Vilkår(
                    behandlingId = behandlingId,
                    status = VilkårStatus.NY,
                    type = nyttVilkår.vilkårType(),
                    erFremtidigUtgift = false,
                    delvilkårwrapper = DelvilkårWrapper(emptyList()),
                    opphavsvilkår = null,
                    gitVersjon = Applikasjonsversjon.versjon,
                )
            vilkårRepository.insert(vilkår.medVurderinger(nyttVilkår))
        }

    @Transactional
    fun oppdaterVilkår(
        nyttVilkår: LagreVilkårReiseOppstartAvslutningHjemreise,
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
    ): Vilkår =
        advisoryLockService.lockForTransaction("roah-midlertidig-bosted:$behandlingId") {
            val eksisterende = hentOgValiderVilkår(behandlingId, vilkårId)
            brukerfeilHvis(
                (eksisterende.type == VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE) !=
                    (nyttVilkår.fakta is FaktaMidlertidigBostedPeriode),
            ) { "Kan ikke bytte mellom reise og midlertidig bostedsperiode" }
            validerLagring(behandlingId, nyttVilkår, vilkårId)
            vilkårRepository.update(eksisterende.medVurderinger(nyttVilkår))
        }

    @Transactional
    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        slettetKommentar: String?,
    ): SlettetVilkårResultat =
        advisoryLockService.lockForTransaction("roah-midlertidig-bosted:$behandlingId") {
            validerBehandling(behandlingService.hentSaksbehandling(behandlingId))
            validerFeatureToggle()
            hentOgValiderVilkår(behandlingId, vilkårId)
            vilkårService.slettVilkår(SlettVilkårRequest(vilkårId, behandlingId, slettetKommentar))
        }

    private fun hentOgValiderVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
    ): Vilkår {
        val vilkår = vilkårRepository.findByIdOrThrow(vilkårId)
        brukerfeilHvis(vilkår.behandlingId != behandlingId) { "Vilkåret tilhører ikke behandlingen" }
        brukerfeilHvisIkke(
            vilkår.type == VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE || vilkår.type.erROAHReise(),
        ) { "Feil vilkårtype for operasjonen" }
        brukerfeilHvis(vilkår.status == VilkårStatus.SLETTET) { "Vilkåret er allerede slettet" }
        return vilkår
    }

    private fun validerLagring(
        behandlingId: BehandlingId,
        request: LagreVilkårReiseOppstartAvslutningHjemreise,
        egenId: VilkårId? = null,
    ) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFeatureToggle()
        brukerfeilHvisIkke(behandling.stønadstype in VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE.gjelderStønader) {
            "Vilkåret gjelder kun reise oppstart, avslutning og hjemreise"
        }
        validerAktivitet(request.fakta.aktivitetId, request.fom, request.tom, behandlingId)
        if (request.fakta !is FaktaMidlertidigBostedPeriode) return
        val overlapper =
            vilkårRepository.findByBehandlingId(behandlingId).any {
                it.type == VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE &&
                    it.id != egenId && it.status != VilkårStatus.SLETTET &&
                    (it.fakta as FaktaROAHMidlertidigBostedPeriode).aktivitetId == request.fakta.aktivitetId &&
                    requireNotNull(it.fom) <= request.tom && requireNotNull(it.tom) >= request.fom
            }
        brukerfeilHvis(overlapper) { "Midlertidige bostedsperioder på samme aktivitet kan ikke overlappe" }
    }

    private fun LagreVilkårReiseOppstartAvslutningHjemreise.vilkårType(): VilkårType =
        when (val fakta = fakta) {
            is FaktaMidlertidigBostedPeriode -> VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE
            is ReiseFaktaROAH -> fakta.typeReiseformål.tilVilkårType()
        }

    private fun Vilkår.medVurderinger(request: LagreVilkårReiseOppstartAvslutningHjemreise): Vilkår {
        val regel =
            when (val fakta = request.fakta) {
                is FaktaMidlertidigBostedPeriode -> MidlertidigBostedPeriodeRegel()
                is ReiseFaktaROAH -> ROAHRegelUtil.finnRelevantVilkårsregel(fakta.typeReiseformål)
            }
        val delvilkår =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = regel,
                svar = request.svar,
            )
        return copy(
            fom = request.fom,
            tom = request.tom,
            type = request.vilkårType(),
            fakta = request.fakta.mapTilVilkårFakta(),
            delvilkårwrapper = DelvilkårWrapper(delvilkår),
            resultat = RegelEvaluering.utledVilkårResultat(delvilkår),
            status = if (status == VilkårStatus.UENDRET) VilkårStatus.ENDRET else status,
        )
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
        aktivitetId: VilkårperiodeGlobalId,
        fom: LocalDate,
        tom: LocalDate,
        behandlingId: BehandlingId,
    ) {
        val aktivitet = vilkårperiodeService.hentAktivitet(aktivitetId, behandlingId)

        brukerfeilHvis(aktivitet == null) {
            "Aktiviteten finnes ikke"
        }
        brukerfeilHvis(aktivitet.resultat != ResultatVilkårperiode.OPPFYLT) {
            "Aktiviteten er ikke oppfylt"
        }
        brukerfeilHvisIkke(aktivitet.fom <= fom && aktivitet.tom >= tom) {
            "Aktiviteten er ikke oppfylt hele vilkårperioden"
        }
    }
}
