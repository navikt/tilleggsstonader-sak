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
