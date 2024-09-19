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
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringResponse
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringRequestDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SimuleringServiceTest {

    private val iverksettClient = mockk<IverksettClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val simuleringsresultatRepository = mockk<SimuleringsresultatRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val tilgangService = mockk<TilgangService>()
    private val iverksettService = mockk<IverksettService>()

    private val simuleringService = SimuleringService(
        iverksettClient = iverksettClient,
        simuleringsresultatRepository = simuleringsresultatRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        tilgangService = tilgangService,
        iverksettService = iverksettService,
    )

    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak
        every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
        every { tilgangService.harTilgangTilRolle(any()) } returns true
        every { iverksettService.forrigeIverksetting(any(), any()) } returns null
    }

    @Test
    internal fun `skal bruke lagret tilkjentYtelse for simulering`() {
        val forrigeBehandlingId = behandling(fagsak).id
        val behandling = behandling(
            fagsak = fagsak,
            forrigeBehandlingId = forrigeBehandlingId,
        )

        val saksbehandling = saksbehandling(fagsak, behandling)
        val tilkjentYtelse = tilkjentYtelse(behandlingId = saksbehandling.id)
        val simuleringsresultat = Simuleringsresultat(
            behandlingId = saksbehandling.id,
            data = SimuleringResponse(mockk(), mockk()),
        )
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs
        every { simuleringsresultatRepository.insert(any()) } returns simuleringsresultat

        val simulerSlot = slot<SimuleringRequestDto>()
        every {
            iverksettClient.simuler(capture(simulerSlot))
        } returns SimuleringResponseDto(mockk(), mockk())

        simuleringService.hentOgLagreSimuleringsresultat(saksbehandling)

        assertThat(simulerSlot.captured.behandlingId).isEqualTo(saksbehandling.eksternId.toString())
        assertThat(simulerSlot.captured.utbetalinger).hasSize(1)
        assertThat(simulerSlot.captured.utbetalinger.first().beløp).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
        assertThat(simulerSlot.captured.utbetalinger.first().fraOgMedDato).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().fom)
        assertThat(simulerSlot.captured.utbetalinger.first().tilOgMedDato).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().tom)
        assertThat(simulerSlot.captured.forrigeIverksetting).isEqualTo(null)
    }

    @Test
    internal fun `skal berike simlueringsresultat`() {
        val forrigeBehandling = behandling(fagsak)
        val behandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            forrigeBehandlingId = forrigeBehandling.id,
        )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id)

        every { iverksettClient.simuler(any()) } returns
            objectMapper.readValue(readFile("mock/iverksett/simuleringsresultat.json"))

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs

        val simulerSlot = slot<Simuleringsresultat>()
        every { simuleringsresultatRepository.insert(capture(simulerSlot)) } answers { firstArg() }

        simuleringService.hentOgLagreSimuleringsresultat(saksbehandling(id = behandling.id))

        assertThat(simulerSlot.captured.data!!.oppsummeringer).hasSize(16)
    }
}
