package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaArenaMapper.mapFaktaArena
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.arenaStatusDto
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.vedtakStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktaArenaMapperTest {

    @Nested
    inner class FinnesVedtak {

        @Test
        fun `finnes ikke vedtak hvis harVedtak=false og harVedtakUtenUtfall=false`() {
            assertThat(mapFaktaArena(arenaStatusDto()).finnesVedtak).isFalse
        }

        @Test
        fun `finnes vedtak hvis harVedtak eller harVedtakUtenUtfall er true`() {
            listOf(
                Pair(true, false),
                Pair(false, true),
                Pair(true, true),
            ).forEach {
                val vedtakStatus = vedtakStatus(harVedtak = it.first, harVedtakUtenUtfall = it.second)
                assertThat(mapFaktaArena(arenaStatusDto(vedtakStatus)).finnesVedtak).isTrue
            }
        }
    }

    @Nested
    inner class VedtakTom {
        @Test
        fun `skal mappe vedtakTom fra vedtakStatus`() {
            listOf(LocalDate.now(), null).forEach {
                assertThat(mapFaktaArena(arenaStatusDto(vedtakStatus(vedtakTom = it))).vedtakTom)
                    .isEqualTo(it)
            }
        }

        @Test
        fun `skal mappe vedtakTom hvis det er innen 3mnd då det ikke er interessant å vise vedtak som slutter før 3mnd bak i tiden`() {
            val dato3mndsiden = LocalDate.now().minusMonths(3)

            assertThat(mapFaktaArena(arenaStatusDto(vedtakStatus(vedtakTom = dato3mndsiden.minusDays(1)))).vedtakTom).isNull()

            listOf(dato3mndsiden, dato3mndsiden.plusDays(1)).forEach {
                assertThat(mapFaktaArena(arenaStatusDto(vedtakStatus(vedtakTom = it))).vedtakTom).isEqualTo(it)
            }
        }
    }
}
