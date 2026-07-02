package no.nav.tilleggsstonader.sak.utbetaling.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsaker
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import no.nav.tilleggsstonader.sak.util.forrigeVirkedag
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatException
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

internal class VarselVedMotregningISimuleringServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private val varselVedMotregningISimuleringService =
        VarselVedMotregningISimuleringService(
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            tilkjentYtelseService = tilkjentYtelseService,
        )

    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN).copy(utbetalPåNyttFagområde = true)

    @Test
    fun `true når annen iverksetting på samme fagområde er i dag`() {
        val behandlingId = behandling(fagsak).id
        val fagsakDagligReiseTso =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.DAGLIG_REISE_TSO).copy(
                utbetalPåNyttFagområde = true,
            )
        val fagsakDagligReiseTsr =
            fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.DAGLIG_REISE_TSR).copy(
                utbetalPåNyttFagområde = true,
            )

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

        val resultat = varselVedMotregningISimuleringService.finnesUtbetalingerPåSammeFagområdeSomIkkeErRegistrertIUR(behandlingId)

        assertThat(resultat).isTrue
    }

    @Test
    fun `false når annen iverksetting på samme fagområde ikke er i dag`() {
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

        val resultat = varselVedMotregningISimuleringService.finnesUtbetalingerPåSammeFagområdeSomIkkeErRegistrertIUR(behandlingId)

        assertThat(resultat).isFalse()
    }

    @Test
    fun `true når det er iverksatt forrige virkedag på en annen fagsak på gammelt fagområde`() {
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

        val resultat = varselVedMotregningISimuleringService.finnesUtbetalingerPåSammeFagområdeSomIkkeErRegistrertIUR(behandlingId)

        assertThat(resultat).isTrue
    }

    @Test
    fun `false når det er iverksatt på en annen fagsak på gammel fagområde for 10 dager siden`() {
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

        val resultat = varselVedMotregningISimuleringService.finnesUtbetalingerPåSammeFagområdeSomIkkeErRegistrertIUR(behandlingId)

        assertThat(resultat).isFalse
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

        val resultat = varselVedMotregningISimuleringService.finnesUtbetalingerPåSammeFagområdeSomIkkeErRegistrertIUR(behandlingId)

        assertThat(resultat).isFalse
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

        val resultat = varselVedMotregningISimuleringService.finnesUtbetalingerPåSammeFagområdeSomIkkeErRegistrertIUR(behandlingId)

        assertThat(resultat).isTrue
    }

    @Test
    fun `skal feile når utbetalPåNyttFagområde mangler for tilsynbarn læremidler og boutgifter`() {
        val fagsakTilsynbarn = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.BARNETILSYN)
        val behandlingId = behandling(fagsakTilsynbarn).id

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsakTilsynbarn
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns
            Fagsaker(mapOf(fagsakTilsynbarn.stønadstype to fagsakTilsynbarn))

        assertThatException()
            .isThrownBy {
                varselVedMotregningISimuleringService.finnesUtbetalingerPåSammeFagområdeSomIkkeErRegistrertIUR(behandlingId)
            }.withMessage("Forventer at utbetalPåNyttFagområde skal være satt på fagsaken")
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
