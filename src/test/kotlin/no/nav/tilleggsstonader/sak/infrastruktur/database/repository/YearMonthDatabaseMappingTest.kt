package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.YearMonth

class YearMonthDatabaseMappingTest : IntegrationTest() {

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `skal håndtere YearMonth som første dagen i måneden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
        val måned = YearMonth.of(2024, 1)
        val andel = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            fom = måned.atDay(1),
            tom = måned.atDay(1),
            utbetalingsmåned = måned,
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, andel))

        val andelFraBasen = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse.single()
        assertThat(andelFraBasen.utbetalingsmåned).isEqualTo(måned)

        val sql = "select utbetalingsmaned::text utbetalingsmaned from andel_tilkjent_ytelse"
        val utbetalingsmaned = jdbcTemplate.query(sql) { rs, rowNum ->
            rs.getString("utbetalingsmaned")
        }.single()
        assertThat(utbetalingsmaned).isEqualTo("2024-01-01")
    }

    @Test
    fun `parsing skal feile hvis dagen i måneden er annerledes enn første dagen i måneden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
        val måned = YearMonth.of(2024, 1)
        val andel = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            fom = måned.atDay(1),
            tom = måned.atDay(1),
            utbetalingsmåned = måned,
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, andel))

        val sql = "update andel_tilkjent_ytelse SET utbetalingsmaned=to_date('2024-01-02', 'yyyy-MM-dd')"
        jdbcTemplate.update(sql, emptyMap<String, String>())

        assertThatThrownBy {
            tilkjentYtelseRepository.findByBehandlingId(behandling.id)
        }.hasRootCauseMessage("Forventer at datoet er første dagen i gitt måned (2024-01-02)")
    }
}
