package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.faktaOffentligTransportReiseTilSamling
import no.nav.tilleggsstonader.sak.util.faktaPrivatBilReiseTilSamling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.LagreVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReiseTilSamlingVilkårServiceTest {
    val vilkårRepository = mockk<VilkårRepository>()
    val vilkårService = mockk<VilkårService>()
    val behandlingService = mockk<BehandlingService>()
    val unleashService = mockk<UnleashService>()
    val vilkårperiodeService = mockk<VilkårperiodeService>(relaxed = true)

    val reiseTilSamlingVilkårService =
        ReiseTilSamlingVilkårService(
            vilkårRepository = vilkårRepository,
            behandlingService = behandlingService,
            vilkårService = vilkårService,
            vilkårperiodeService = vilkårperiodeService,
            unleashService = unleashService,
        )

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelse(svar = SvarId.JA, begrunnelse = "begrunnelse"),
            RegelId.DOKUMENTERTE_UTGIFTER to SvarOgBegrunnelse(svar = SvarId.JA, begrunnelse = "begrunnelse"),
            RegelId.DEKKET_AV_ANNET_STIPEND to SvarOgBegrunnelse(svar = SvarId.NEI),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelse(svar = SvarId.JA),
        )

    val svarPrivatBil =
        mapOf(
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelse(svar = SvarId.JA, begrunnelse = "begrunnelse"),
            RegelId.DOKUMENTERTE_UTGIFTER to SvarOgBegrunnelse(svar = SvarId.JA, begrunnelse = "begrunnelse"),
            RegelId.DEKKET_AV_ANNET_STIPEND to SvarOgBegrunnelse(svar = SvarId.NEI),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelse(svar = SvarId.NEI, begrunnelse = "begrunnelse"),
            RegelId.KAN_REISE_MED_EGEN_BIL to SvarOgBegrunnelse(svar = SvarId.JA),
        )

    val nyttVilkår =
        LagreVilkårReiseTilSamling(
            fom = 1 januar 2025,
            tom = 31 januar 2025,
            svar = svarOffentligTransport,
            fakta =
                faktaOffentligTransportReiseTilSamling().let {
                    FaktaOffentligTransport(
                        reiseId = it.reiseId,
                        adresse = it.adresse,
                        utgifterOffentligTransport = it.utgifterOffentligTransport,
                        aktivitetId = it.aktivitetId,
                    )
                },
        )

    @Test
    fun `skal ikke kunne opprette vilkår når behandlingen er låst for redigering`() {
        val behandling = saksbehandling(status = BehandlingStatus.FERDIGSTILT)
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                reiseTilSamlingVilkårService.opprettNyttVilkår(
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
                reiseTilSamlingVilkårService.oppdaterVilkår(
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
                reiseTilSamlingVilkårService.opprettNyttVilkår(
                    nyttVilkår = nyttVilkår,
                    behandlingId = BehandlingId.random(),
                )
            }.withMessage("Kan ikke oppdatere vilkår når behandling er på steg=INNGANGSVILKÅR.")
    }

    @Test
    fun `skal feile hvis feature toggle for reise til samling ikke er skrudd på`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns false

        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                reiseTilSamlingVilkårService.opprettNyttVilkår(
                    nyttVilkår = nyttVilkår,
                    behandlingId = behandling.id,
                )
            }.withMessage("TS-sak støtter foreløpig ikke behandling av saker som gjelder reise til samling")
    }

    @Test
    fun `skal validere aktivitet for offentlig transport ved opprettelse for TSR`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val aktivitetId = VilkårperiodeGlobalId.random()
        val aktivitet = mockk<VilkårperiodeAktivitet>(relaxed = true)
        every { aktivitet.resultat } returns ResultatVilkårperiode.OPPFYLT
        every { aktivitet.inneholder(any<Periode<LocalDate>>()) } returns true
        every { vilkårperiodeService.hentAktivitet(aktivitetId, any()) } returns aktivitet

        val vilkår =
            nyttVilkår.copy(
                fakta =
                    FaktaOffentligTransport(
                        reiseId = dummyReiseId,
                        adresse = "Samlingsveien 1",
                        utgifterOffentligTransport = 500.toBigDecimal(),
                        aktivitetId = aktivitetId,
                    ),
            )

        reiseTilSamlingVilkårService.opprettNyttVilkår(
            nyttVilkår = vilkår,
            behandlingId = behandling.id,
        )

        verify(exactly = 1) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal feile når aktivitetId mangler for offentlig transport TSR`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true

        val vilkår =
            nyttVilkår.copy(
                fakta =
                    FaktaOffentligTransport(
                        reiseId = dummyReiseId,
                        adresse = "Samlingsveien 1",
                        utgifterOffentligTransport = 500.toBigDecimal(),
                        aktivitetId = null,
                    ),
            )

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                reiseTilSamlingVilkårService.opprettNyttVilkår(
                    nyttVilkår = vilkår,
                    behandlingId = behandling.id,
                )
            }.withMessage("Aktivitet må velges")
    }

    @Test
    fun `skal ikke kreve aktivitetId for offentlig transport TSO`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val vilkår =
            nyttVilkår.copy(
                fakta =
                    FaktaOffentligTransport(
                        reiseId = dummyReiseId,
                        adresse = "Samlingsveien 1",
                        utgifterOffentligTransport = 500.toBigDecimal(),
                        aktivitetId = null,
                    ),
            )

        reiseTilSamlingVilkårService.opprettNyttVilkår(
            nyttVilkår = vilkår,
            behandlingId = behandling.id,
        )

        verify(exactly = 0) {
            vilkårperiodeService.hentAktivitet(any(), any())
        }
    }

    @Test
    fun `skal feile når aktivitet ikke finnes for privat bil`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true
        every { vilkårperiodeService.hentAktivitet(any(), any()) } returns null

        val vilkår =
            nyttVilkår.copy(
                svar = svarPrivatBil,
                fakta =
                    faktaPrivatBilReiseTilSamling().let {
                        FaktaPrivatBil(
                            reiseId = it.reiseId,
                            adresse = it.adresse,
                            reiseavstand = it.reiseavstand,
                            aktivitetId = it.aktivitetId,
                        )
                    },
            )

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                reiseTilSamlingVilkårService.opprettNyttVilkår(
                    nyttVilkår = vilkår,
                    behandlingId = behandling.id,
                )
            }.withMessage("Aktiviteten finnes ikke")
    }

    @Test
    fun `skal feile når aktivitet ikke er oppfylt for privat bil`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true

        val aktivitet = mockk<VilkårperiodeAktivitet>(relaxed = true)
        every { aktivitet.resultat } returns ResultatVilkårperiode.IKKE_OPPFYLT
        every { vilkårperiodeService.hentAktivitet(any(), any()) } returns aktivitet

        val vilkår =
            nyttVilkår.copy(
                svar = svarPrivatBil,
                fakta =
                    faktaPrivatBilReiseTilSamling().let {
                        FaktaPrivatBil(
                            reiseId = it.reiseId,
                            adresse = it.adresse,
                            reiseavstand = it.reiseavstand,
                            aktivitetId = it.aktivitetId,
                        )
                    },
            )

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                reiseTilSamlingVilkårService.opprettNyttVilkår(
                    nyttVilkår = vilkår,
                    behandlingId = behandling.id,
                )
            }.withMessage("Aktiviteten er ikke oppfylt")
    }

    @Test
    fun `skal feile når aktivitet ikke dekker hele vilkårperioden for privat bil`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true

        val aktivitet = mockk<VilkårperiodeAktivitet>(relaxed = true)
        every { aktivitet.resultat } returns ResultatVilkårperiode.OPPFYLT
        every { aktivitet.fom } returns (1 januar 2025)
        every { aktivitet.tom } returns (15 januar 2025)
        every { aktivitet.inneholder(any<Periode<LocalDate>>()) } returns false
        every { vilkårperiodeService.hentAktivitet(any(), any()) } returns aktivitet

        val vilkår =
            nyttVilkår.copy(
                svar = svarPrivatBil,
                fakta =
                    faktaPrivatBilReiseTilSamling().let {
                        FaktaPrivatBil(
                            reiseId = it.reiseId,
                            adresse = it.adresse,
                            reiseavstand = it.reiseavstand,
                            aktivitetId = it.aktivitetId,
                        )
                    },
            )

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                reiseTilSamlingVilkårService.opprettNyttVilkår(
                    nyttVilkår = vilkår,
                    behandlingId = behandling.id,
                )
            }.withMessage("Aktiviteten er ikke oppfylt hele vilkårperioden")
    }

    @Test
    fun `skal kunne opprette vilkår for privat bil når aktiviteten er oppfylt og dekker hele perioden`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        every { unleashService.isEnabled(any()) } returns true
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val aktivitetId = VilkårperiodeGlobalId.random()
        val aktivitet = mockk<VilkårperiodeAktivitet>(relaxed = true)
        every { aktivitet.resultat } returns ResultatVilkårperiode.OPPFYLT
        every { aktivitet.fom } returns (1 januar 2025)
        every { aktivitet.tom } returns (31 januar 2025)
        every { aktivitet.inneholder(any<Periode<LocalDate>>()) } returns true
        every { vilkårperiodeService.hentAktivitet(aktivitetId, any()) } returns aktivitet

        val vilkår =
            nyttVilkår.copy(
                svar = svarPrivatBil,
                fakta =
                    FaktaPrivatBil(
                        reiseId = dummyReiseId,
                        adresse = "Samlingsveien 1",
                        reiseavstand = 40.toBigDecimal(),
                        aktivitetId = aktivitetId,
                    ),
            )

        reiseTilSamlingVilkårService.opprettNyttVilkår(
            nyttVilkår = vilkår,
            behandlingId = behandling.id,
        )

        verify(exactly = 1) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal feile når reiseavstand er mindre enn 30 km`() {
        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                FaktaPrivatBil(
                    reiseId = dummyReiseId,
                    adresse = "Samlingsveien 1",
                    reiseavstand = 20.toBigDecimal(),
                    aktivitetId = null,
                )
            }.withMessage("Reiseavstand kan ikke være mindre enn 30 km")
    }
}
