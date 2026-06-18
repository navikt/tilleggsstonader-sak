package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DagligReiseVilkårServiceTest {
    val vilkårRepository = mockk<VilkårRepository>()
    val vilkårService = mockk<VilkårService>()
    val behandlingService = mockk<BehandlingService>()
    val unleashService = mockk<UnleashService>()
    val vilkårperiodeService = mockk<VilkårperiodeService>(relaxed = true)

    val dagligReiseVilkårService =
        DagligReiseVilkårService(
            vilkårRepository = vilkårRepository,
            behandlingService = behandlingService,
            vilkårService = vilkårService,
            unleashService = unleashService,
            vilkårperiodeService = vilkårperiodeService,
        )

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelse(svar = SvarId.JA, begrunnelse = "begrunnelse"),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to
                SvarOgBegrunnelse(
                    svar = SvarId.JA,
                    begrunnelse = "begrunnelse",
                ),
        )

    val nyttVilkår =
        LagreVilkårDagligReise(
            fom = 1 januar 2025,
            tom = 31 januar 2025,
            svar = svarOffentligTransport,
            fakta = faktaOffentligTransport(),
        )

    @Test
    fun `skal ikke kunne opprette vilkår når behandlingen er låst for redigering`() {
        val behandling = saksbehandling(status = BehandlingStatus.FERDIGSTILT)
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                dagligReiseVilkårService.opprettNyttVilkår(
                    nyttVilkår = nyttVilkår,
                    behandlingId = BehandlingId.random(),
                )
            }.withMessage("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
    }

    @Test
    fun `skal ikke kunne endre vilkår når behandlingen er låst for redigering`() {
        val behandling = saksbehandling(status = BehandlingStatus.FERDIGSTILT)
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                dagligReiseVilkårService.oppdaterVilkår(
                    nyttVilkår = nyttVilkår,
                    behandlingId = BehandlingId.random(),
                    vilkårId = VilkårId.random(),
                )
            }.withMessage("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
    }

    @Test
    fun `skal ikke kunne opprette vilkår når behandlingen ikke er i vilkårsteget`() {
        val behandling = saksbehandling(steg = StegType.INNGANGSVILKÅR)
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                dagligReiseVilkårService.opprettNyttVilkår(
                    nyttVilkår = nyttVilkår,
                    behandlingId = BehandlingId.random(),
                )
            }.withMessage("Kan ikke oppdatere vilkår når behandling er på steg=INNGANGSVILKÅR.")
    }

    @Test
    fun `skal validere aktivitet for offentlig transport ved opprettelse`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSR))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val vilkår =
            nyttVilkår.copy(
                fakta =
                    FaktaOffentligTransport(
                        reiseId = dummyReiseId,
                        adresse = "Tiltaksveien 1",
                        reisedagerPerUke = 5,
                        prisEnkelbillett = 40,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = 800,
                        tiltaksvariant = TypeAktivitet.GRUPPEAMO,
                    ),
            )

        dagligReiseVilkårService.opprettNyttVilkår(
            nyttVilkår = vilkår,
            behandlingId = behandling.id,
        )

        verify {
            vilkårperiodeService.validerAktivitetMedTiltaksvariantInnenforPeriode(
                tiltaksvariant = TypeAktivitet.GRUPPEAMO,
                periode = Datoperiode(fom = 1 januar 2025, tom = 31 januar 2025),
                behandlingId = behandling.id,
            )
        }
    }

    @Test
    fun `skal ikke kunne endre vilkår når behandlingen ikke er i vilkårsteget`() {
        val behandling = saksbehandling(steg = StegType.INNGANGSVILKÅR)
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                dagligReiseVilkårService.oppdaterVilkår(
                    nyttVilkår = nyttVilkår,
                    behandlingId = BehandlingId.random(),
                    vilkårId = VilkårId.random(),
                )
            }.withMessage("Kan ikke oppdatere vilkår når behandling er på steg=INNGANGSVILKÅR.")
    }

    fun faktaOffentligTransport(
        reiseId: ReiseId = dummyReiseId,
        adresse: String? = "Tiltaksveien 1",
        reisedagerPerUke: Int = 5,
        prisEnkelbillett: Int? = 40,
        prisSyvdagersbillett: Int? = null,
        prisTrettidagersbillett: Int? = 800,
    ) = FaktaOffentligTransport(
        reiseId = reiseId,
        adresse = adresse,
        reisedagerPerUke = reisedagerPerUke,
        prisEnkelbillett = prisEnkelbillett,
        prisSyvdagersbillett = prisSyvdagersbillett,
        prisTrettidagersbillett = prisTrettidagersbillett,
    )

    @Test
    fun `harPrivatBilVilkår skal returnere false når ingen vilkår finnes`() {
        val behandlingId = BehandlingId.random()
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns emptyList()

        val resultat = dagligReiseVilkårService.harPrivatBilVilkår(behandlingId, null)

        assertThat(resultat).isFalse
    }

    @Test
    fun `harPrivatBilVilkår skal returnere false når kun offentlig transport vilkår finnes`() {
        val behandlingId = BehandlingId.random()
        val vilkår =
            lagVilkårMedFakta(
                FaktaDagligReiseOffentligTransport(
                    reiseId = dummyReiseId,
                    reisedagerPerUke = 5,
                    prisEnkelbillett = 40,
                    prisSyvdagersbillett = null,
                    prisTrettidagersbillett = 800,
                    adresse = "test",
                ),
            )
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns listOf(vilkår)

        val resultat = dagligReiseVilkårService.harPrivatBilVilkår(behandlingId, null)

        assertThat(resultat).isFalse
    }

    @Test
    fun `harPrivatBilVilkår skal returnere true når privat bil vilkår finnes i nåværende behandling`() {
        val behandlingId = BehandlingId.random()
        val privatBilVilkår =
            lagVilkårMedFakta(
                FaktaDagligReisePrivatBil(
                    reiseId = dummyReiseId,
                    reiseavstandEnVei = BigDecimal("10"),
                    faktaDelperioder = emptyList(),
                    adresse = "test",
                    aktivitetId =
                        VilkårperiodeGlobalId
                            .random(),
                ),
            )
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns listOf(privatBilVilkår)

        val resultat = dagligReiseVilkårService.harPrivatBilVilkår(behandlingId, null)

        assertThat(resultat).isTrue
    }

    @Test
    fun `harPrivatBilVilkår skal returnere true når blanding av offentlig og privat vilkår finnes`() {
        val behandlingId = BehandlingId.random()
        val offentligVilkår =
            lagVilkårMedFakta(
                FaktaDagligReiseOffentligTransport(
                    reiseId = ReiseId.random(),
                    reisedagerPerUke = 5,
                    prisEnkelbillett = 40,
                    prisSyvdagersbillett = null,
                    prisTrettidagersbillett = 800,
                    adresse = "test1",
                ),
            )
        val privatBilVilkår =
            lagVilkårMedFakta(
                FaktaDagligReisePrivatBil(
                    reiseId = dummyReiseId,
                    reiseavstandEnVei = java.math.BigDecimal("10"),
                    faktaDelperioder = emptyList(),
                    adresse = "test2",
                    aktivitetId =
                        VilkårperiodeGlobalId
                            .random(),
                ),
            )
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns listOf(offentligVilkår, privatBilVilkår)

        val resultat = dagligReiseVilkårService.harPrivatBilVilkår(behandlingId, null)

        assertThat(resultat).isTrue
    }

    @Test
    fun `harPrivatBilVilkår skal returnere true når forrige behandling har privat bil`() {
        val behandlingId = BehandlingId.random()
        val forrigeBehandlingId = BehandlingId.random()
        val offentligVilkår =
            lagVilkårMedFakta(
                FaktaDagligReiseOffentligTransport(
                    reiseId = dummyReiseId,
                    reisedagerPerUke = 5,
                    prisEnkelbillett = 40,
                    prisSyvdagersbillett = null,
                    prisTrettidagersbillett = 800,
                    adresse = "test",
                ),
            )
        val privatBilVilkår =
            lagVilkårMedFakta(
                FaktaDagligReisePrivatBil(
                    reiseId = ReiseId.random(),
                    reiseavstandEnVei = java.math.BigDecimal("10"),
                    faktaDelperioder = emptyList(),
                    adresse = "test",
                    aktivitetId =
                        VilkårperiodeGlobalId
                            .random(),
                ),
            )
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns listOf(offentligVilkår)
        every { vilkårRepository.findByBehandlingId(forrigeBehandlingId) } returns listOf(privatBilVilkår)

        val resultat = dagligReiseVilkårService.harPrivatBilVilkår(behandlingId, forrigeBehandlingId)

        assertThat(resultat).isTrue
    }

    private fun lagVilkårMedFakta(fakta: VilkårFakta): Vilkår =
        Vilkår(
            id = VilkårId.random(),
            behandlingId = BehandlingId.random(),
            type = VilkårType.DAGLIG_REISE,
            status = VilkårStatus.NY,
            erFremtidigUtgift = false,
            delvilkårwrapper = DelvilkårWrapper(emptyList()),
            opphavsvilkår = null,
            gitVersjon = null,
            fakta = fakta,
        )
}
