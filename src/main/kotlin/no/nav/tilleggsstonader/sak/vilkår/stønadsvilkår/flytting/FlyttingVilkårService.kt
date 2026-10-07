package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.feilHvisIkke
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingRegel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlyttingVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val unleashService: UnleashService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<Vilkår> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .filter { it.type == VilkårType.FLYTTING }
            .sortedBy { it.fom }

    @Transactional
    fun opprettVilkår(
        behandlingId: BehandlingId,
        innsendt: LagreVilkårFlytting,
    ): Vilkår {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFaktaOgSvar(innsendt)
        val vilkår = byggVilkår(behandlingId, innsendt)
        validerIngenOverlapp(behandlingId, vilkår)
        return vilkårRepository.insert(vilkår)
    }

    @Transactional
    fun oppdaterVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        innsendt: LagreVilkårFlytting,
    ): Vilkår {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFaktaOgSvar(innsendt)
        val eksisterende = vilkårRepository.findByIdOrThrow(vilkårId)
        validerEierskapOgStatus(eksisterende, behandlingId)
        val oppdatert = byggVilkår(behandlingId, innsendt, eksisterende)
        validerIngenOverlapp(behandlingId, oppdatert, vilkårId)
        return vilkårRepository.update(oppdatert)
    }

    @Transactional
    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        kommentar: String?,
    ): SlettetVilkårResultat {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        val eksisterende = vilkårRepository.findByIdOrThrow(vilkårId)
        validerEierskapOgStatus(eksisterende, behandlingId)
        return vilkårService.slettVilkår(
            SlettVilkårRequest(id = vilkårId, behandlingId = behandlingId, kommentar = kommentar),
        )
    }

    private fun byggVilkår(
        behandlingId: BehandlingId,
        innsendt: LagreVilkårFlytting,
        eksisterende: Vilkår? = null,
    ): Vilkår {
        brukerfeilHvis(innsendt.fom.isAfter(innsendt.tom)) { "Fra-dato må være før eller lik til-dato" }
        val delvilkår =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = FlyttingRegel(),
                svar = innsendt.svar.mapValues { it.value },
            )
        val fakta = innsendt.fakta
        val samletResultat =
            if (FlyttingVilkårValidering.erFullstendig(innsendt)) {
                RegelEvaluering.utledVilkårResultat(delvilkår)
            } else {
                Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }
        return eksisterende?.copy(
            resultat = samletResultat,
            status = utledStatus(eksisterende),
            fom = innsendt.fom,
            tom = innsendt.tom,
            delvilkårwrapper = DelvilkårWrapper(delvilkår),
            fakta = fakta,
        ) ?: Vilkår(
            behandlingId = behandlingId,
            resultat = samletResultat,
            status = VilkårStatus.NY,
            type = VilkårType.FLYTTING,
            fom = innsendt.fom,
            tom = innsendt.tom,
            erFremtidigUtgift = false,
            delvilkårwrapper = DelvilkårWrapper(delvilkår),
            opphavsvilkår = null,
            gitVersjon = Applikasjonsversjon.versjon,
            fakta = fakta,
        )
    }

    private fun validerFaktaOgSvar(innsendt: LagreVilkårFlytting) {
        FlyttingVilkårValidering.validerOppgitteFakta(innsendt.fakta)
        val flyttemåte = innsendt.svar[RegelId.HVORDAN_SKAL_BRUKER_FLYTTE]?.svar

        brukerfeilHvis(flyttemåte != null && flyttemåte !in setOf(SvarId.FLYTTEBYRÅ, SvarId.FLYTTER_SELV)) {
            "Flyttevilkår må besvares med flyttebyrå eller flytter selv"
        }

        when (flyttemåte) {
            null -> {
                brukerfeilHvis(innsendt.fakta !is FaktaFlyttingUbestemt) {
                    "Vurderingen av flyttemåte må besvares før fakta registreres"
                }
            }

            SvarId.FLYTTEBYRÅ -> {
                brukerfeilHvis(innsendt.fakta !is FaktaFlyttebyrå) {
                    "Fakta må inneholde to tilbud fra flyttebyrå når flyttebyrå er valgt"
                }
            }

            SvarId.FLYTTER_SELV -> {
                brukerfeilHvis(innsendt.fakta !is FaktaFlytteSelv) {
                    "Fakta må inneholde avstand og kostnader når bruker flytter selv"
                }
            }

            else -> error("Ugyldig svar på flyttemåte")
        }
    }

    private fun validerIngenOverlapp(
        behandlingId: BehandlingId,
        nyttVilkår: Vilkår,
        unntattVilkårId: VilkårId? = null,
    ) {
        val periodeFom = nyttVilkår.fom ?: error("Forventer fom")
        val periodeTom = nyttVilkår.tom ?: error("Forventer tom")
        val overlapper =
            vilkårRepository
                .findByBehandlingId(behandlingId)
                .filter {
                    it.type == VilkårType.FLYTTING &&
                        it.status != VilkårStatus.SLETTET &&
                        it.id != unntattVilkårId
                }.any { eksisterende ->
                    val eksisterendeFom = eksisterende.fom ?: return@any false
                    val eksisterendeTom = eksisterende.tom ?: return@any false
                    !periodeFom.isAfter(eksisterendeTom) && !eksisterendeFom.isAfter(periodeTom)
                }
        brukerfeilHvis(overlapper) { "Flyttevilkår kan ikke ha overlappende perioder" }
    }

    private fun validerBehandling(behandling: Saksbehandling) {
        feilHvisIkke(behandling.steg == StegType.VILKÅR) {
            "Kan ikke endre flyttevilkår når behandlingen er på steg=${behandling.steg}"
        }
        behandling.status.validerKanBehandlingRedigeres()
        feilHvisIkke(behandling.stønadstype in setOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR)) {
            "Flyttevilkår kan bare registreres på flyttebehandlinger"
        }
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING)) {
            "Behandling av flyttevilkår er ikke aktivert"
        }
    }

    private fun validerEierskapOgStatus(
        vilkår: Vilkår,
        behandlingId: BehandlingId,
    ) {
        feilHvisIkke(vilkår.behandlingId == behandlingId && vilkår.type == VilkårType.FLYTTING) {
            "Flyttevilkåret tilhører ikke behandlingen"
        }
        brukerfeilHvis(vilkår.status == VilkårStatus.SLETTET) { "Flyttevilkåret er allerede slettet" }
    }

    private fun utledStatus(eksisterende: Vilkår): VilkårStatus? =
        when (eksisterende.status) {
            VilkårStatus.UENDRET -> VilkårStatus.ENDRET
            else -> eksisterende.status
        }
}
