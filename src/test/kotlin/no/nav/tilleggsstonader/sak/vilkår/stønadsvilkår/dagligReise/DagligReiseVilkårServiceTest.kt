package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class DagligReiseVilkårServiceTest {
    val vilkårRepository = mockk<VilkårRepository>()
    val vilkårService = mockk<VilkårService>()
    val behandlingService = mockk<BehandlingService>()
    val unleashService = mockk<UnleashService>()

    val dagligReiseVilkårService =
        DagligReiseVilkårService(
            vilkårRepository = vilkårRepository,
            behandlingService = behandlingService,
            vilkårService = vilkårService,
            unleashService = unleashService,
        )

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelse(svar = SvarId.JA),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelse(svar = SvarId.JA),
        )

    val nyttVilkår =
        LagreDagligReise(
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
}
