package no.nav.tilleggsstonader.sak.utbetaling.simulering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsaker
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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDetaljer as SimuleringDetaljerKontrakt

internal class SimuleringServiceTest {
    private val simuleringClient = mockk<SimuleringClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val simuleringsresultatRepository = mockk<SimuleringsresultatRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val tilgangService = mockk<TilgangService>()
    private val fagsakUtbetalingIdService = mockk<FagsakUtbetalingIdService>()
    private val fagsakUtbetalingsvalgService = mockk<FagsakUtbetalingsvalgService>()

    private val utbetalingV3Mapper = UtbetalingV3Mapper(fagsakUtbetalingIdService, fagsakUtbetalingsvalgService, tilkjentYtelseService)

    private val simuleringService =
        SimuleringService(
            simuleringClient = simuleringClient,
            simuleringsresultatRepository = simuleringsresultatRepository,
            tilkjentYtelseService = tilkjentYtelseService,
            tilgangService = tilgangService,
            utbetalingV3Mapper = utbetalingV3Mapper,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
        )

    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN).copy(utbetalPåNyttFagområde = true)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak
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
        every { behandlingService.hentBehandling(any()) } returns behandling
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

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs

        val simulerSlot = slot<Simuleringsresultat>()
        every { simuleringsresultatRepository.insert(capture(simulerSlot)) } answers { firstArg() }

        simuleringService.hentOgLagreSimuleringsresultat(saksbehandling(id = behandling.id))

        assertThat(simulerSlot.captured.data!!.oppsummeringer).hasSize(16)
    }

    @Test
    fun `skal sende varsel for daglig reise når iverksetting er idag`() {
        val behandlingId = behandling(fagsak).id
        val fagsakDagligReiseTso =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.DAGLIG_REISE_TSO).copy(
                utbetalPåNyttFagområde = true,
            )
        val fagsakDagligReiseTsr =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.DAGLIG_REISE_TSR).copy(
                utbetalPåNyttFagområde = true,
            )
        val varselTekst = "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"

        val alleFagsaker =
            Fagsaker(listOf(fagsakDagligReiseTso, fagsakDagligReiseTsr).associateBy { it.stønadstype })
        val idag = LocalDate.now()

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakDagligReiseTso
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns alleFagsaker

        val behandling = behandling(fagsakDagligReiseTso)
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling

        val tilkjentYtelse =
            tilkjentYtelse(
                behandlingId = behandling.id,
                andeler =
                    listOf(
                        tilkjentYtelse(
                            behandlingId = behandlingId,
                        ).andelerTilkjentYtelse.first().copy(
                            iverksetting =
                                mockk {
                                    every { iverksettingTidspunkt } returns idag.atStartOfDay()
                                },
                        ),
                    ).toTypedArray(),
            )

        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse

        val resultat = simuleringService.lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId)

        assertThat(resultat).isEqualTo(varselTekst)
    }

    @Test
    fun `skal sende varsel for daglig reise når iverksetting ikke er i dag`() {
        val behandlingId = behandling(fagsak).id
        val fagsakDagligReiseTso =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.DAGLIG_REISE_TSO).copy(
                utbetalPåNyttFagområde = true,
            )
        val fagsakDagligReiseTsr =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.DAGLIG_REISE_TSR).copy(
                utbetalPåNyttFagområde = true,
            )

        val alleFagsaker = Fagsaker(listOf(fagsakDagligReiseTso, fagsakDagligReiseTsr).associateBy { it.stønadstype })
        val idag = LocalDate.now()

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakDagligReiseTso
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns alleFagsaker

        val behandling = behandling(fagsakDagligReiseTso)
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling

        val tilkjentYtelse =
            tilkjentYtelse(
                behandlingId = behandling.id,
                andeler =
                    listOf(
                        tilkjentYtelse(
                            behandlingId = behandlingId,
                        ).andelerTilkjentYtelse.first().copy(
                            iverksetting =
                                mockk {
                                    every { iverksettingTidspunkt } returns idag.atStartOfDay().minusDays(10)
                                },
                        ),
                    ).toTypedArray(),
            )

        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse

        val resultat = simuleringService.lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId)

        assertThat(resultat).isNull()
    }

    @Test
    fun `skal sende varsel for tilsynbarn når dato er innenfor periode og det utbetales på gammelt fagområde`() {
        val behandlingId = behandling(fagsak).id

        val fagsakTilsynbarn =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN).copy(
                utbetalPåNyttFagområde = false,
            )
        val fagsakLæremidler =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.LÆREMIDLER).copy(
                utbetalPåNyttFagområde = false,
            )

        val alleFagsaker =
            Fagsaker(listOf(fagsakTilsynbarn, fagsakLæremidler).associateBy { it.stønadstype })
        val varselTekst = "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakTilsynbarn
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns alleFagsaker

        val behandling = behandling(fagsakTilsynbarn)
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        val idag = LocalDate.now()

        val datoInnenfor = idag.forrigeVirkedag()

        val tilkjentYtelse =
            tilkjentYtelse(
                behandlingId = behandling.id,
                andeler =
                    listOf(
                        tilkjentYtelse(behandlingId = behandlingId).andelerTilkjentYtelse.first().copy(
                            iverksetting =
                                mockk {
                                    every { iverksettingTidspunkt } returns datoInnenfor.atStartOfDay()
                                },
                        ),
                    ).toTypedArray(),
            )

        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse

        val resultat = simuleringService.lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId)

        assertThat(resultat).isEqualTo(varselTekst)
    }

    @Test
    fun `skal ikke sende varsel for tilsynbarn når dato er utenfor periode og det utbetales på gammelt fagområde`() {
        val behandlingId = behandling(fagsak).id

        val fagsakTilsynbarn =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN).copy(
                utbetalPåNyttFagområde = false,
            )

        val alleFagsaker = Fagsaker(mapOf(fagsakTilsynbarn.stønadstype to fagsakTilsynbarn))

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakTilsynbarn
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns alleFagsaker

        val behandling = behandling(fagsakTilsynbarn)
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        val idag = LocalDate.now()
        val datoUtenfor = idag.forrigeVirkedag().minusDays(10)

        val tilkjentYtelse =
            tilkjentYtelse(
                behandlingId = behandling.id,
                andeler =
                    listOf(
                        tilkjentYtelse(behandlingId = behandlingId).andelerTilkjentYtelse.first().copy(
                            iverksetting =
                                mockk {
                                    every { iverksettingTidspunkt } returns datoUtenfor.atStartOfDay()
                                },
                        ),
                    ).toTypedArray(),
            )

        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse

        val resultat = simuleringService.lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId)

        assertThat(resultat).isNull()
    }

    @Test
    fun `skal kun sjekke samme fagsak for tilsynbarn når nytt fagområde brukes`() {
        val fagsakTilsynbarn = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN).copy(utbetalPåNyttFagområde = true)
        val fagsakLæremidler = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.LÆREMIDLER).copy(utbetalPåNyttFagområde = false)
        val behandlingId = behandling(fagsakTilsynbarn).id

        val alleFagsaker = Fagsaker(listOf(fagsakTilsynbarn, fagsakLæremidler).associateBy { it.stønadstype })

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakTilsynbarn
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns alleFagsaker
        every { behandlingService.finnSisteIverksatteBehandling(fagsakTilsynbarn.id) } returns null
        every { behandlingService.finnSisteIverksatteBehandling(fagsakLæremidler.id) } returns behandling(fagsakLæremidler)

        val resultat = simuleringService.lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId)

        assertThat(resultat).isNull()
    }

    @Test
    fun `skal sjekke alle fagsaker med gammelt fagområde`() {
        val fagsakTilsynbarn = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN).copy(utbetalPåNyttFagområde = false)
        val fagsakLæremidler = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.LÆREMIDLER).copy(utbetalPåNyttFagområde = true)
        val fagsakBoutgifter = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BOUTGIFTER).copy(utbetalPåNyttFagområde = true)
        val fagsakDagligReise =
            fagsak(
                fagsakpersoner(setOf(personIdent)),
                Stønadstype.DAGLIG_REISE_TSO,
            ).copy(utbetalPåNyttFagområde = false)
        val behandlingId = behandling(fagsakTilsynbarn).id

        val alleFagsaker =
            Fagsaker(
                listOf(
                    fagsakTilsynbarn,
                    fagsakLæremidler,
                    fagsakBoutgifter,
                    fagsakDagligReise,
                ).associateBy { it.stønadstype },
            )
        val varselTekst = "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"
        val behandlingDagligReise = behandling(fagsakDagligReise)
        val idag = LocalDate.now()

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakTilsynbarn
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns alleFagsaker
        every { behandlingService.finnSisteIverksatteBehandling(fagsakTilsynbarn.id) } returns null
        every { behandlingService.finnSisteIverksatteBehandling(fagsakLæremidler.id) } returns null
        every { behandlingService.finnSisteIverksatteBehandling(fagsakBoutgifter.id) } returns null
        every { behandlingService.finnSisteIverksatteBehandling(fagsakDagligReise.id) } returns behandlingDagligReise
        every { tilkjentYtelseService.hentForBehandling(behandlingDagligReise.id) } returns
            tilkjentYtelse(
                behandlingId = behandlingDagligReise.id,
                andeler =
                    listOf(
                        tilkjentYtelse(behandlingId = behandlingDagligReise.id).andelerTilkjentYtelse.first().copy(
                            iverksetting =
                                mockk {
                                    every { iverksettingTidspunkt } returns idag.atStartOfDay()
                                },
                        ),
                    ).toTypedArray(),
            )

        val resultat = simuleringService.lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId)

        assertThat(resultat).isEqualTo(varselTekst)
    }

    @Test
    fun `skal feile når utbetalPåNyttFagområde mangler for tilsynbarn læremidler og boutgifter`() {
        val fagsakTilsynbarn = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN)
        val behandlingId = behandling(fagsakTilsynbarn).id

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakTilsynbarn
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns
            Fagsaker(mapOf(fagsakTilsynbarn.stønadstype to fagsakTilsynbarn))

        assertThatThrownBy { simuleringService.lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId) }
            .hasMessageContaining("Forventer at utbetalPåNyttFagområde skal være satt på fagsaken")
    }

    @Test
    fun `forrigeVirkedag skal gi riktig dag`() {
        // Mandag -> forrige virkedag er Fredag
        assertThat(LocalDate.of(2026, 3, 30).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(LocalDate.of(2026, 3, 30).forrigeVirkedag()).isEqualTo(LocalDate.of(2026, 3, 27))

        // Sondag -> forrige virkedag er Fredag
        assertThat(LocalDate.of(2026, 3, 29).dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(LocalDate.of(2026, 3, 29).forrigeVirkedag()).isEqualTo(LocalDate.of(2026, 3, 27))

        // Onsdag -> forrige virkedag er Tirsdag
        assertThat(LocalDate.of(2026, 3, 25).dayOfWeek).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(LocalDate.of(2026, 3, 25).forrigeVirkedag()).isEqualTo(LocalDate.of(2026, 3, 24))
    }
}
