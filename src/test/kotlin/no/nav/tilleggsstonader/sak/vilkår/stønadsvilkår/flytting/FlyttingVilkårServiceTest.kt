package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import java.util.Optional

class FlyttingVilkårServiceTest {
    private val vilkårRepository = mockk<VilkårRepository>()
    private val vilkårService = mockk<VilkårService>()
    private val behandlingService = mockk<BehandlingService>()
    private val unleashService = mockk<UnleashService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()

    init {
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(emptyList(), emptyList())
    }

    private val flyttingVilkårService =
        FlyttingVilkårService(
            vilkårRepository = vilkårRepository,
            behandlingService = behandlingService,
            vilkårService = vilkårService,
            unleashService = unleashService,
            vilkårperiodeService = vilkårperiodeService,
        )

    private val nyttVilkår =
        LagreVilkårFlytting(
            flyttedato = 1 januar 2026,
            svar = emptyMap(),
            fakta = FaktaFlyttingUbestemt(adresse = "Adresse 1"),
        )

    private val eksisterendeVilkår =
        VilkårFlytting(
            behandlingId = BehandlingId.random(),
            flyttedato = 1 januar 2026,
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            status = VilkårStatus.NY,
            delvilkårsett = emptyList(),
            fakta = FaktaFlyttingUbestemt(adresse = "Adresse 1"),
        )

    @Test
    fun `skal kunne opprette ufullstendig flyttevilkår for TSO`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns emptyList()
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val resultat =
            flyttingVilkårService.opprettVilkår(
                behandlingId = behandling.id,
                innsendt = nyttVilkår,
            )

        assertThat(resultat.behandlingId).isEqualTo(behandling.id)
        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        assertThat(resultat.flyttedato).isEqualTo(nyttVilkår.flyttedato)
        assertThat(resultat.fakta.adresse).isEqualTo(nyttVilkår.fakta.adresse)
        verify(exactly = 1) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal avvise flytteforbindelse for enslig forsørger eller gjenlevende uten overlappende målgruppe`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val annenMålgruppe =
            målgruppe(
                behandlingId = behandling.id,
                fom = nyttVilkår.flyttedato,
                tom = nyttVilkår.flyttedato,
                faktaOgVurdering = faktaOgVurderingMålgruppe(MålgruppeType.AAP),
            )
        val innsendt =
            nyttVilkår.copy(
                svar =
                    mapOf(
                        RegelId.HAR_UTGIFTER_TIL_FLYTTING to SvarOgBegrunnelse(SvarId.JA),
                        RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                            SvarOgBegrunnelse(
                                SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID,
                                begrunnelse = "Flytter for å starte i arbeid",
                            ),
                    ),
            )
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårperiodeService.hentVilkårperioder(behandling.id) } returns
            Vilkårperioder(listOf(annenMålgruppe), emptyList())
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns emptyList()
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                flyttingVilkårService.opprettVilkår(
                    behandlingId = behandling.id,
                    innsendt = innsendt,
                )
            }.withMessage(
                "Du kan bare velge svaret «${SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID.beskrivelse}» " +
                    "sammen med en målgruppeperiode for aktiv eller opphørt overgangs- eller omstillingsstønad som inneholder flyttedatoen",
            )

        verify(exactly = 0) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal avvise flytteforbindelse for enslig forsørger eller gjenlevende når målgruppeperioden ikke overlapper`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val overgangsstønad =
            målgruppe(
                behandlingId = behandling.id,
                fom = 2 januar 2026,
                tom = 2 januar 2026,
                faktaOgVurdering = faktaOgVurderingMålgruppe(MålgruppeType.OVERGANGSSTØNAD),
            )
        val innsendt =
            nyttVilkår.copy(
                svar =
                    mapOf(
                        RegelId.HAR_UTGIFTER_TIL_FLYTTING to SvarOgBegrunnelse(SvarId.JA),
                        RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                            SvarOgBegrunnelse(
                                SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID,
                                begrunnelse = "Flytter for å starte i arbeid",
                            ),
                    ),
            )
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårperiodeService.hentVilkårperioder(behandling.id) } returns
            Vilkårperioder(listOf(overgangsstønad), emptyList())

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                flyttingVilkårService.opprettVilkår(
                    behandlingId = behandling.id,
                    innsendt = innsendt,
                )
            }.withMessage(
                "Du kan bare velge svaret «${SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID.beskrivelse}» " +
                    "sammen med en målgruppeperiode for aktiv eller opphørt overgangs- eller omstillingsstønad som inneholder flyttedatoen",
            )

        verify(exactly = 0) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal godta flytteforbindelse for enslig forsørger eller gjenlevende med overlappende overgangsstønad`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val overgangsstønad =
            målgruppe(
                behandlingId = behandling.id,
                fom = nyttVilkår.flyttedato,
                tom = nyttVilkår.flyttedato,
                faktaOgVurdering = faktaOgVurderingMålgruppe(MålgruppeType.OVERGANGSSTØNAD),
            )
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårperiodeService.hentVilkårperioder(behandling.id) } returns
            Vilkårperioder(listOf(overgangsstønad), emptyList())
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns emptyList()
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val resultat =
            flyttingVilkårService.opprettVilkår(
                behandlingId = behandling.id,
                innsendt =
                    nyttVilkår.copy(
                        svar =
                            mapOf(
                                RegelId.HAR_UTGIFTER_TIL_FLYTTING to SvarOgBegrunnelse(SvarId.JA),
                                RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                                    SvarOgBegrunnelse(
                                        SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID,
                                        begrunnelse = "Flytter for å starte i arbeid",
                                    ),
                            ),
                    ),
            )

        assertThat(resultat.behandlingId).isEqualTo(behandling.id)
        verify(exactly = 1) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal markere uendret vilkår som endret ved oppdatering`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val vilkår = eksisterendeVilkår.copy(behandlingId = behandling.id, status = VilkårStatus.UENDRET)
        val oppdatering = nyttVilkår.copy(flyttedato = 20 januar 2026)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårRepository.findById(vilkår.id) } returns Optional.of(vilkår.mapTilVilkår())
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns listOf(vilkår.mapTilVilkår())
        every { vilkårRepository.update(any<Vilkår>()) } answers { firstArg() }

        val resultat =
            flyttingVilkårService.oppdaterVilkår(
                behandlingId = behandling.id,
                vilkårId = vilkår.id,
                innsendt = oppdatering,
            )

        assertThat(resultat.id).isEqualTo(vilkår.id)
        assertThat(resultat.status).isEqualTo(VilkårStatus.ENDRET)
        assertThat(resultat.tom).isEqualTo(oppdatering.flyttedato)
        verify(exactly = 1) { vilkårRepository.update(any<Vilkår>()) }
    }

    @Test
    fun `skal ikke kunne opprette vilkår med samme flyttedato periode`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val vilkår = eksisterendeVilkår.copy(behandlingId = behandling.id)
        val overlappendeVilkår = nyttVilkår.copy(flyttedato = vilkår.fom)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns listOf(vilkår.mapTilVilkår())

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                flyttingVilkårService.opprettVilkår(
                    behandlingId = behandling.id,
                    innsendt = overlappendeVilkår,
                )
            }.withMessage("Flyttevilkår kan ikke ha overlappende perioder")

        verify(exactly = 0) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal kunne opprette vilkår med periode som følger etter eksisterende vilkår`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val vilkår = eksisterendeVilkår.copy(behandlingId = behandling.id)
        val tilgrensendeVilkår = nyttVilkår.copy(flyttedato = 16 januar 2026)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns listOf(vilkår.mapTilVilkår())
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val resultat =
            flyttingVilkårService.opprettVilkår(
                behandlingId = behandling.id,
                innsendt = tilgrensendeVilkår,
            )

        assertThat(resultat.fom).isEqualTo(tilgrensendeVilkår.flyttedato)
        assertThat(resultat.tom).isEqualTo(tilgrensendeVilkår.flyttedato)
        verify(exactly = 1) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal kunne opprette vilkår med periode som overlapper slettet vilkår`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val slettetVilkår =
            eksisterendeVilkår.copy(
                behandlingId = behandling.id,
                status = VilkårStatus.SLETTET,
                resultat = Vilkårsresultat.SLETTET,
                slettetKommentar = "Feilregistrert",
            )
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns listOf(slettetVilkår.mapTilVilkår())
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val resultat =
            flyttingVilkårService.opprettVilkår(
                behandlingId = behandling.id,
                innsendt = nyttVilkår,
            )

        assertThat(resultat.fom).isEqualTo(slettetVilkår.fom)
        assertThat(resultat.tom).isEqualTo(slettetVilkår.tom)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        verify(exactly = 1) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal ikke kunne opprette vilkår når behandlingen er låst for redigering`() {
        val behandling = saksbehandling(steg = StegType.VILKÅR, status = BehandlingStatus.FERDIGSTILT)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                flyttingVilkårService.opprettVilkår(
                    behandlingId = behandling.id,
                    innsendt = nyttVilkår,
                )
            }.withMessage("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
    }

    @Test
    fun `skal ikke kunne endre vilkår når behandlingen er låst for redigering`() {
        val behandling = saksbehandling(steg = StegType.VILKÅR, status = BehandlingStatus.FERDIGSTILT)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                flyttingVilkårService.oppdaterVilkår(
                    behandlingId = behandling.id,
                    vilkårId = eksisterendeVilkår.id,
                    innsendt = nyttVilkår,
                )
            }.withMessage("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
    }

    @Test
    fun `skal ikke kunne opprette vilkår når behandlingen ikke er i vilkårsteget`() {
        val behandling = saksbehandling(steg = StegType.INNGANGSVILKÅR)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                flyttingVilkårService.opprettVilkår(
                    behandlingId = behandling.id,
                    innsendt = nyttVilkår,
                )
            }.withMessage("Kan ikke endre flyttevilkår når behandlingen er på steg=INNGANGSVILKÅR")
    }

    @Test
    fun `skal ikke kunne endre vilkår når behandlingen ikke er i vilkårsteget`() {
        val behandling = saksbehandling(steg = StegType.INNGANGSVILKÅR)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                flyttingVilkårService.oppdaterVilkår(
                    behandlingId = behandling.id,
                    vilkårId = eksisterendeVilkår.id,
                    innsendt = nyttVilkår,
                )
            }.withMessage("Kan ikke endre flyttevilkår når behandlingen er på steg=INNGANGSVILKÅR")
    }
}
