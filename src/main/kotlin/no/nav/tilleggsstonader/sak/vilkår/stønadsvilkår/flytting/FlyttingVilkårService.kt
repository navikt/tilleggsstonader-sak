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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingRegel
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlyttingVilkårService(
    private val vilkårRepository: VilkårRepository,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val unleashService: UnleashService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun hentVilkårForBehandling(behandlingId: BehandlingId): List<VilkårFlytting> =
        vilkårRepository
            .findByBehandlingId(behandlingId)
            .filter { it.type == VilkårType.FLYTTING }
            .map { it.mapTilVilkårFlytting() }
            .sortedBy { it.fom }

    fun hentOppfylteVilkårforBehandling(behandlingId: BehandlingId): List<VilkårFlytting> =
        hentVilkårForBehandling(behandlingId).filter { it.resultat == Vilkårsresultat.OPPFYLT }

    @Transactional
    fun opprettVilkår(
        behandlingId: BehandlingId,
        innsendt: LagreVilkårFlytting,
    ): VilkårFlytting {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFaktaOgSvar(innsendt)
        validerMålgruppeOgFlyttesvar(behandlingId, innsendt)
        val vilkår = byggVilkår(behandlingId, innsendt)
        validerIngenOverlapp(behandlingId, vilkår)
        return vilkårRepository.insert(vilkår.mapTilVilkår()).mapTilVilkårFlytting()
    }

    @Transactional
    fun oppdaterVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        innsendt: LagreVilkårFlytting,
    ): VilkårFlytting {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerFaktaOgSvar(innsendt)
        validerMålgruppeOgFlyttesvar(behandlingId, innsendt)
        val eksisterende = vilkårRepository.findByIdOrThrow(vilkårId).mapTilVilkårFlytting()
        validerEierskapOgStatus(eksisterende, behandlingId)
        val oppdatert = byggVilkår(behandlingId, innsendt, eksisterende)
        validerIngenOverlapp(behandlingId, oppdatert, vilkårId)
        return vilkårRepository.update(oppdatert.mapTilVilkår()).mapTilVilkårFlytting()
    }

    @Transactional
    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        kommentar: String?,
    ): SlettetVilkårResultat =
        vilkårService.slettVilkår(
            SlettVilkårRequest(
                id = vilkårId,
                behandlingId = behandlingId,
                kommentar = kommentar,
            ),
        )

    private fun byggVilkår(
        behandlingId: BehandlingId,
        innsendt: LagreVilkårFlytting,
        eksisterende: VilkårFlytting? = null,
    ): VilkårFlytting {
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
            fom = innsendt.flytteDato,
            tom = innsendt.flytteDato,
            delvilkårsett = delvilkår,
            fakta = fakta,
        ) ?: VilkårFlytting(
            behandlingId = behandlingId,
            resultat = samletResultat,
            status = VilkårStatus.NY,
            fom = innsendt.flytteDato,
            tom = innsendt.flytteDato,
            delvilkårsett = delvilkår,
            fakta = fakta,
        )
    }

    private fun validerFaktaOgSvar(innsendt: LagreVilkårFlytting) {
        FlyttingVilkårValidering.validerOppgitteFakta(innsendt.fakta)
    }

    private fun validerMålgruppeOgFlyttesvar(
        behandlingId: BehandlingId,
        innsendt: LagreVilkårFlytting,
    ) {
        val valgtFlytteforbindelse = innsendt.svar[RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER]?.svar

        if (valgtFlytteforbindelse == SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID) {
            val godkjenteMålgrupperForSvar =
                vilkårperiodeService.hentVilkårperioder(behandlingId).målgrupper.filter {
                    it.status != Vilkårstatus.SLETTET &&
                        it.type in
                        setOf(
                            MålgruppeType.OPPHØRT_OMSTILLINGSSTØNAD,
                            MålgruppeType.OMSTILLINGSSTØNAD,
                            MålgruppeType.OPPHØRT_OVERGANGSSTØNAD,
                            MålgruppeType.OVERGANGSSTØNAD,
                        )
                }

            val flytteDato = innsendt.flytteDato

            brukerfeilHvis(
                godkjenteMålgrupperForSvar.none { it.inneholder(flytteDato) },
            ) {
                "Du kan bare velge svaret «${SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID.beskrivelse}» " +
                    "sammen med en målgruppeperiode for aktiv eller opphørt overgangs- eller omstillingsstønad som inneholder flyttedatoen"
            }
        }
    }

    private fun validerIngenOverlapp(
        behandlingId: BehandlingId,
        nyttVilkår: VilkårFlytting,
        unntattVilkårId: VilkårId? = null,
    ) {
        val overlapper =
            vilkårRepository
                .findByBehandlingId(behandlingId)
                .filter {
                    it.type == VilkårType.FLYTTING &&
                        it.status != VilkårStatus.SLETTET &&
                        it.id != unntattVilkårId
                }.any { nyttVilkår.overlapper(it.mapTilVilkårFlytting()) }
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
        vilkår: VilkårFlytting,
        behandlingId: BehandlingId,
    ) {
        feilHvisIkke(vilkår.behandlingId == behandlingId) {
            "Flyttevilkåret tilhører ikke behandlingen"
        }
        brukerfeilHvis(vilkår.status == VilkårStatus.SLETTET) { "Flyttevilkåret er allerede slettet" }
    }

    private fun utledStatus(eksisterende: VilkårFlytting): VilkårStatus? =
        when (eksisterende.status) {
            VilkårStatus.UENDRET -> VilkårStatus.ENDRET
            else -> eksisterende.status
        }
}
