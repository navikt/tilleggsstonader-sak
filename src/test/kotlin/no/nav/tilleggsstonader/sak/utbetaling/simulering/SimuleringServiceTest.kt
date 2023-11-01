package no.nav.tilleggsstonader.sak.utbetaling.simulering

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.iverksett.IverksettClient
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.BeriketSimuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.tilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

internal class SimuleringServiceTest {

    private val iverksettClient = mockk<IverksettClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val simuleringsresultatRepository = mockk<SimuleringsresultatRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val tilgangService = mockk<TilgangService>()

    private val simuleringService = SimuleringService(
        iverksettClient = iverksettClient,
        simuleringsresultatRepository = simuleringsresultatRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        tilgangService = tilgangService,
    )

    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak
        every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
        every { tilgangService.harTilgangTilRolle(any()) } returns true
    }

    @Test
    internal fun `skal bruke lagret tilkjentYtelse for simulering`() {
        val forrigeBehandlingId = behandling(fagsak).id
        val behandling = behandling(
            fagsak = fagsak,
            forrigeBehandlingId = forrigeBehandlingId,
        )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id)
        val simuleringsresultat = Simuleringsresultat(
            behandlingId = behandling.id,
            data = BeriketSimuleringsresultat(mockk(), mockk()),
        )
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs
        every { simuleringsresultatRepository.insert(any()) } returns simuleringsresultat

        val simulerSlot = slot<SimuleringDto>()
        every {
            iverksettClient.simuler(capture(simulerSlot))
        } returns BeriketSimuleringsresultat(mockk(), mockk())
        simuleringService.simuler(saksbehandling(fagsak, behandling))

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
            .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().periode.fom.atDay(1))
            .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().periode.tom.atEndOfMonth())
            .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().stønadTom)
        assertThat(simulerSlot.captured.forrigeBehandlingId).isEqualTo(forrigeBehandlingId)
    }

    @Test
    internal fun `skal feile hvis behandlingen ikke er redigerbar og mangler lagret simulering`() {
        val behandling =
            behandling(
                fagsak = fagsak,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.FATTER_VEDTAK,
            )
        every { behandlingService.hentBehandling(any()) } returns behandling
        assertThrows<RuntimeException> {
            simuleringService.simuler(saksbehandling(id = behandling.id))
        }
    }

    @Test
    internal fun `skal hente lagret simulering hvis behandlingen ikke er redigerbar`() {
        val behandling =
            behandling(
                fagsak = fagsak,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.FATTER_VEDTAK,
            )
        every { behandlingService.hentBehandling(any()) } returns behandling
        every {
            simuleringsresultatRepository.findByIdOrNull(behandling.id)
        } returns Simuleringsresultat(
            behandlingId = behandling.id,
            data = BeriketSimuleringsresultat(mockk(), mockk()),
        )
        val simuleringsresultatDto = simuleringService.simuler(saksbehandling(fagsak, behandling))
        assertThat(simuleringsresultatDto).isNotNull
    }

    @Test
    internal fun `skal berike simlueringsresultat`() {
        val forrigeBehandlingId = behandling(fagsak).id
        val behandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            forrigeBehandlingId = forrigeBehandlingId,
        )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id)

        every { iverksettClient.simuler(any()) } returns
            objectMapper.readValue(readFile("mock/iverksett/simuleringsresultat_beriket.json"))

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs

        val simulerSlot = slot<Simuleringsresultat>()
        every { simuleringsresultatRepository.insert(capture(simulerSlot)) } answers { firstArg() }

        simuleringService.simuler(saksbehandling(id = behandling.id))

        assertThat(simulerSlot.captured.data.oppsummering.fom)
            .isEqualTo(LocalDate.of(2021, 2, 1))
    }
}
