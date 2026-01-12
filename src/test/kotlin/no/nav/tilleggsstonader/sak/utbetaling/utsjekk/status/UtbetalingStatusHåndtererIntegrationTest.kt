package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired

class UtbetalingStatusHåndtererIntegrationTest(
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val utbetalingStatusHåndterer: UtbetalingStatusHåndterer,
) : CleanDatabaseIntegrationTest() {
    @ParameterizedTest(name = "status {0} fra Helved oppdaterer andeler med IverksettingStatus {0}")
    @CsvSource(
        "MOTTATT, MOTTATT",
        "OK, OK",
        "HOS_OPPDRAG, HOS_OPPDRAG",
    )
    fun `status oppdaterer andeler korrekt`(
        statusFraHelved: UtbetalingStatus,
        forventetStatus: StatusIverksetting,
    ) {
        // Gjennomfør behandling til iverksetting er ferdig
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                medVilkår = listOf(lagreDagligReiseDto()),
                tilSteg = StegType.BEHANDLING_FERDIGSTILT,
            )

        // Hent iverksettingId fra andel
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        assertThat(tilkjentYtelse).isNotNull
        val andelerFørStatus = tilkjentYtelse!!.andelerTilkjentYtelse
        assertThat(andelerFørStatus).isNotEmpty
        val iverksettingId = andelerFørStatus.first().iverksetting?.iverksettingId
        assertThat(iverksettingId).isEqualTo(behandlingId.id)

        val statusRecord =
            UtbetalingStatusRecord(
                status = statusFraHelved,
                detaljer =
                    UtbetalingStatusDetaljer(
                        ytelse = "TILLSTDR",
                        linjer = emptyList(),
                    ),
                error = null,
            )
        utbetalingStatusHåndterer.behandleStatusoppdatering(
            iverksettingId.toString(),
            statusRecord,
            UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )

        // Andeler skal være oppdatert med korrekt status
        val tilkjentYtelseEtter = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        assertThat(tilkjentYtelseEtter!!.andelerTilkjentYtelse).allMatch {
            it.statusIverksetting == forventetStatus
        }
    }

    @Test
    fun `FEILET status oppdaterer andeler med FEILET status og inkluderer error detaljer`() {
        val behandlingId = opprettBehandlingOgGjennomførBehandlingsløp()

        // Hent iverksettingId fra andel
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        assertThat(tilkjentYtelse).isNotNull
        val andelerFørStatus = tilkjentYtelse!!.andelerTilkjentYtelse
        assertThat(andelerFørStatus).isNotEmpty
        val iverksettingId = andelerFørStatus.first().iverksetting?.iverksettingId
        assertThat(iverksettingId).isEqualTo(behandlingId.id)

        val statusRecord =
            UtbetalingStatusRecord(
                status = UtbetalingStatus.FEILET,
                detaljer =
                    UtbetalingStatusDetaljer(
                        ytelse = "TILLSTDR",
                        linjer = emptyList(),
                    ),
                error =
                    UtbetalingError(
                        statusCode = 400,
                        msg = "OPPDRAGET/FAGSYSTEM-ID finnes ikke fra før",
                        doc = "https://helved-docs.ansatt.dev.nav.no/v3/doc/",
                    ),
            )
        utbetalingStatusHåndterer.behandleStatusoppdatering(
            iverksettingId.toString(),
            statusRecord,
            UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )

        // SJekk at andeler er oppdatert med FEILET-status
        val tilkjentYtelseEtter = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        assertThat(tilkjentYtelseEtter!!.andelerTilkjentYtelse).allMatch {
            it.statusIverksetting == StatusIverksetting.FEILET
        }
    }

    @Test
    fun `flere andeler oppdateres alle når status mottas`() {
        // Opprett behandling med flere andeler
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                medVilkår =
                    listOf(
                        lagreDagligReiseDto(fom = 2 januar 2025, tom = 2 januar 2025),
                        lagreDagligReiseDto(fom = 6 januar 2025, tom = 6 januar 2025),
                    ),
            )

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        val andelerFørStatus = tilkjentYtelse!!.andelerTilkjentYtelse
        assertThat(andelerFørStatus).hasSize(2)
        val iverksettingId = andelerFørStatus.first().iverksetting?.iverksettingId
        assertThat(iverksettingId).isNotNull

        val statusRecord =
            UtbetalingStatusRecord(
                status = UtbetalingStatus.OK,
                detaljer =
                    UtbetalingStatusDetaljer(
                        ytelse = "TILLSTDR",
                        linjer = emptyList(),
                    ),
                error = null,
            )
        utbetalingStatusHåndterer.behandleStatusoppdatering(
            iverksettingId.toString(),
            statusRecord,
            UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )

        // Alle andeler skal være oppdatert
        val tilkjentYtelseEtter = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        val andelerEtter = tilkjentYtelseEtter!!.andelerTilkjentYtelse
        assertThat(andelerEtter).hasSize(2)
        assertThat(andelerEtter).allMatch { it.statusIverksetting == StatusIverksetting.OK }
    }

    @Test
    fun `ignorerer status for andre fagsystemer`() {
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                medAktivitet = ::lagreVilkårperiodeAktivitet,
                medMålgruppe = ::lagreVilkårperiodeMålgruppe,
                medVilkår = listOf(lagreDagligReiseDto()),
                tilSteg = StegType.BEHANDLING_FERDIGSTILT,
            )

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        val andelerFørStatus = tilkjentYtelse!!.andelerTilkjentYtelse
        val originalStatus = andelerFørStatus.first().statusIverksetting

        val statusGammeltFagomrade =
            UtbetalingStatusRecord(
                status = UtbetalingStatus.OK,
                detaljer =
                    UtbetalingStatusDetaljer(
                        ytelse = "DAGPENGER",
                        linjer = emptyList(),
                    ),
                error = null,
            )
        val iverksettingId = andelerFørStatus.first().iverksetting?.iverksettingId
        assertThat(iverksettingId).isNotNull
        utbetalingStatusHåndterer.behandleStatusoppdatering(iverksettingId.toString(), statusGammeltFagomrade, "DAGPENGER")

        // Status skal IKKE endres fordi det er et annet fagsystem
        val tilkjentYtelseEtter = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        val andelerEtter = tilkjentYtelseEtter!!.andelerTilkjentYtelse
        assertThat(andelerEtter).allMatch { it.statusIverksetting == originalStatus }
    }
}
