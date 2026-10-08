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
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import java.util.Optional

class FlyttingVilkårServiceTest {
    private val vilkårRepository = mockk<VilkårRepository>()
    private val vilkårService = mockk<VilkårService>()
    private val behandlingService = mockk<BehandlingService>()
    private val unleashService = mockk<UnleashService>()

    private val flyttingVilkårService =
        FlyttingVilkårService(
            vilkårRepository = vilkårRepository,
            behandlingService = behandlingService,
            vilkårService = vilkårService,
            unleashService = unleashService,
        )

    private val nyttVilkår =
        LagreVilkårFlytting(
            fom = 1 januar 2026,
            tom = 15 januar 2026,
            svar = emptyMap(),
            fakta = FaktaFlyttingUbestemt(adresse = "Adresse 1"),
        )

    private val eksisterendeVilkår =
        VilkårFlytting(
            behandlingId = BehandlingId.random(),
            fom = 1 januar 2026,
            tom = 15 januar 2026,
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
        assertThat(resultat.fom).isEqualTo(nyttVilkår.fom)
        assertThat(resultat.tom).isEqualTo(nyttVilkår.tom)
        assertThat(resultat.fakta).isEqualTo(nyttVilkår.fakta)
        verify(exactly = 1) { vilkårRepository.insert(any<Vilkår>()) }
    }

    @Test
    fun `skal hente flyttevilkår sortert på fra-dato`() {
        val førsteVilkår = eksisterendeVilkår
        val andreVilkår = førsteVilkår.copy(id = VilkårId.random(), fom = 16 januar 2026, tom = 20 januar 2026)
        every { vilkårRepository.findByBehandlingId(førsteVilkår.behandlingId) } returns
            listOf(andreVilkår.mapTilVilkår(), førsteVilkår.mapTilVilkår())

        val resultat =
            flyttingVilkårService.hentVilkårForBehandling(
                behandlingId = førsteVilkår.behandlingId,
            )

        assertThat(resultat).containsExactly(førsteVilkår, andreVilkår)
    }

    @Test
    fun `skal markere uendret vilkår som endret ved oppdatering`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val vilkår = eksisterendeVilkår.copy(behandlingId = behandling.id, status = VilkårStatus.UENDRET)
        val oppdatering = nyttVilkår.copy(tom = 20 januar 2026)
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
        assertThat(resultat.tom).isEqualTo(oppdatering.tom)
        verify(exactly = 1) { vilkårRepository.update(any<Vilkår>()) }
    }

    @Test
    fun `skal ikke kunne opprette vilkår med overlappende periode`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        val vilkår = eksisterendeVilkår.copy(behandlingId = behandling.id)
        val overlappendeVilkår = nyttVilkår.copy(fom = 15 januar 2026, tom = 20 januar 2026)
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
        val tilgrensendeVilkår = nyttVilkår.copy(fom = 16 januar 2026, tom = 20 januar 2026)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        every { vilkårRepository.findByBehandlingId(behandling.id) } returns listOf(vilkår.mapTilVilkår())
        every { vilkårRepository.insert(any<Vilkår>()) } answers { firstArg() }

        val resultat =
            flyttingVilkårService.opprettVilkår(
                behandlingId = behandling.id,
                innsendt = tilgrensendeVilkår,
            )

        assertThat(resultat.fom).isEqualTo(tilgrensendeVilkår.fom)
        assertThat(resultat.tom).isEqualTo(tilgrensendeVilkår.tom)
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
    fun `skal ikke kunne opprette vilkår med fra-dato etter til-dato`() {
        val behandling =
            saksbehandling(steg = StegType.VILKÅR, fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSR))
        val ugyldigVilkår = nyttVilkår.copy(fom = 16 januar 2026)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                flyttingVilkårService.opprettVilkår(
                    behandlingId = behandling.id,
                    innsendt = ugyldigVilkår,
                )
            }.withMessage("Fra-dato må være før eller lik til-dato")

        verify(exactly = 0) { vilkårRepository.insert(any<Vilkår>()) }
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
