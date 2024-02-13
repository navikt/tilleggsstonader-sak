package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class AndelTilkjentYtelseRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Test
    fun `skal kunne oppdatere en enkelt andel`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val andel1 = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            beløp = 100,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 1),
        )
        val andel2 = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            beløp = 200,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 1),
        )
        val tilkjentYtelse = tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = setOf(andel1, andel2),
                startdato = LocalDate.now(),
            ),
        )

        val iverksattAndel2 = andel2.copy(
            status = StatusIverksetting.SENDT,
            iverksetting = Iverksetting(UUID.randomUUID(), SporbarUtils.now()),
        )
        andelTilkjentYtelseRepository.update(iverksattAndel2)

        val tyFraDb = tilkjentYtelseRepository.findByBehandlingId(behandling.id)
        assertThat(tyFraDb).isEqualTo(tilkjentYtelse.copy(andelerTilkjentYtelse = setOf(andel1, iverksattAndel2.copy(version = 1))))

        val andel2FraDb = andelTilkjentYtelseRepository.findByIdOrThrow(andel2.id)
        assertThat(andel2FraDb).isEqualTo(iverksattAndel2.copy(version = 1))
    }

    @Test
    fun `skal oppdatere endret tidspunkt vid oppdateringer`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val andel1 = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            beløp = 100,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 1),
        )
        tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = setOf(andel1),
                startdato = LocalDate.now(),
            ),
        )

        jdbcTemplate.update(
            "UPDATE andel_tilkjent_ytelse SET endret_tid = :tid",
            mapOf("tid" to LocalDate.of(2023, 1, 1).atStartOfDay().truncatedTo(ChronoUnit.MILLIS)),
        )

        val andel = andelTilkjentYtelseRepository.findByIdOrThrow(andel1.id)
        andelTilkjentYtelseRepository.update(andel.copy(status = StatusIverksetting.OK))
        val andelEtterOppdatering = andelTilkjentYtelseRepository.findByIdOrThrow(andel1.id)

        assertThat(andel.endretTid.toLocalDate()).isEqualTo(LocalDate.of(2023, 1, 1))
        assertThat(andelEtterOppdatering.endretTid.toLocalDate()).isEqualTo(LocalDate.now())
    }
}
