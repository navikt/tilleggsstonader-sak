package no.nav.tilleggsstonader.sak.utbetaling.simulering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.fagomrade.FagsakUtbetalingsvalgService
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingId
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.SimuleringClient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringJson
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.SimuleringDto
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingV3Mapper
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import no.nav.tilleggsstonader.sak.util.forrigeVirkedag
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDetaljer as SimuleringDetaljerKontrakt

internal class SimuleringServiceTest {
    private val simuleringClient = mockk<SimuleringClient>()
    private val simuleringsresultatRepository = mockk<SimuleringsresultatRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val tilgangService = mockk<TilgangService>()
    private val fagsakUtbetalingIdService = mockk<FagsakUtbetalingIdService>()
    private val fagsakUtbetalingsvalgService = mockk<FagsakUtbetalingsvalgService>()
    private val varselVedMotregningISimuleringService = mockk<VarselVedMotregningISimuleringService>(relaxed = true)

    private val utbetalingV3Mapper = UtbetalingV3Mapper(fagsakUtbetalingIdService, fagsakUtbetalingsvalgService, tilkjentYtelseService)

    private val simuleringService =
        SimuleringService(
            simuleringClient = simuleringClient,
            simuleringsresultatRepository = simuleringsresultatRepository,
            tilkjentYtelseService = tilkjentYtelseService,
            tilgangService = tilgangService,
            utbetalingV3Mapper = utbetalingV3Mapper,
            varselVedMotregningISimuleringService = varselVedMotregningISimuleringService,
        )

    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN).copy(utbetalPåNyttFagområde = true)

    @BeforeEach
    internal fun setUp() {
        every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
        every { tilgangService.harTilgangTilRolle(any()) } returns true
        every { fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(any(), any(), any()) } answers {
            FagsakUtbetalingId(
                fagsakId = FagsakId(firstArg()),
                typeAndel = secondArg(),
                reiseId = thirdArg(),
            )
        }
        every { fagsakUtbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(any(), any()) } returns true
        every { fagsakUtbetalingIdService.hentUtbetalingIderForFagsakId(any()) } returns
            listOf(
                FagsakUtbetalingId(
                    fagsakId = FagsakId.random(),
                    typeAndel = TypeAndel.TILSYN_BARN_AAP,
                    reiseId = null,
                ),
            )
    }

    @Test
    internal fun `skal bruke lagret tilkjentYtelse for simulering`() {
        val forrigeIverksatteBehandlingId = behandling(fagsak).id
        val behandling =
            behandling(
                fagsak = fagsak,
                forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
            )

        val saksbehandling = saksbehandling(fagsak, behandling)
        val tilkjentYtelse = tilkjentYtelse(behandlingId = saksbehandling.id)
        val simuleringsresultat =
            Simuleringsresultat(
                behandlingId = saksbehandling.id,
                data = SimuleringJson(mockk(), mockk()),
            )
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs
        every { simuleringsresultatRepository.insert(any()) } returns simuleringsresultat

        val simulerSlot = slot<SimuleringDto>()
        val detaljer = SimuleringDetaljerKontrakt("", LocalDate.now(), 0, emptyList())
        every {
            simuleringClient.simuler(capture(simulerSlot))
        } returns SimuleringResponseDto(emptyList(), detaljer)

        simuleringService.hentOgLagreSimuleringsresultat(saksbehandling)

        assertThat(simulerSlot.captured.behandlingId).isEqualTo(saksbehandling.eksternId.toString())
        assertThat(simulerSlot.captured.utbetalinger).hasSize(1)
        assertThat(
            simulerSlot.captured.utbetalinger
                .single()
                .perioder,
        ).hasSize(1)
        assertThat(
            simulerSlot.captured.utbetalinger
                .single()
                .perioder
                .single()
                .beløp
                .toInt(),
        ).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
        assertThat(
            simulerSlot.captured.utbetalinger
                .single()
                .perioder
                .single()
                .fom,
        ).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().fom)
        assertThat(
            simulerSlot.captured.utbetalinger
                .single()
                .perioder
                .single()
                .tom,
        ).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().tom)
    }

    @Test
    internal fun `skal berike simlueringsresultat`() {
        val forrigeBehandling = behandling(fagsak)
        val behandling =
            behandling(
                fagsak = fagsak,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                forrigeIverksatteBehandlingId = forrigeBehandling.id,
            )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id)

        every { simuleringClient.simuler(any()) } returns
            jsonMapper.readValue(readFile("mock/iverksett/simuleringsresultat.json"))

        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs

        val simulerSlot = slot<Simuleringsresultat>()
        every { simuleringsresultatRepository.insert(capture(simulerSlot)) } answers { firstArg() }

        simuleringService.hentOgLagreSimuleringsresultat(saksbehandling(id = behandling.id))

        assertThat(simulerSlot.captured.data!!.oppsummeringer).hasSize(16)
    }

    @Test
    fun `forrigeVirkedag skal gi riktig dag`() {
        assertThat(LocalDate.of(2026, 3, 30).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(LocalDate.of(2026, 3, 30).forrigeVirkedag()).isEqualTo(LocalDate.of(2026, 3, 27))

        assertThat(LocalDate.of(2026, 3, 29).dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(LocalDate.of(2026, 3, 29).forrigeVirkedag()).isEqualTo(LocalDate.of(2026, 3, 27))

        assertThat(LocalDate.of(2026, 3, 25).dayOfWeek).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(LocalDate.of(2026, 3, 25).forrigeVirkedag()).isEqualTo(LocalDate.of(2026, 3, 24))
    }
}
