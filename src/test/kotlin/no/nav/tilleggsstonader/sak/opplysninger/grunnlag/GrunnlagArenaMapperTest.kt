package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.arenaStatusDto
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.vedtakStatus
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagArenaMapper.mapFaktaArena
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrunnlagArenaMapperTest {
    @Nested
    inner class VedtakTom {
        @Test
        fun `skal mappe vedtakTom fra vedtakStatus`() {
            listOf(LocalDate.now(), null).forEach {
                val statusDto = arenaStatusDto(vedtakStatus(vedtakTom = it))
                assertThat(mapFaktaArena(statusDto, Stønadstype.BARNETILSYN).vedtakTom).isEqualTo(it)
            }
        }

        @Test
        fun `skal mappe vedtakTom hvis det er innen 3 plus 2 mnd for tilsyn barn`() {
            val dato3mndsiden = LocalDate.now().minusMonths(5)

            assertThat(
                mapFaktaArena(
                    arenaStatusDto(vedtakStatus(vedtakTom = dato3mndsiden.minusDays(1))),
                    Stønadstype.BARNETILSYN,
                ).vedtakTom,
            ).isNull()

            listOf(dato3mndsiden, dato3mndsiden.plusDays(1)).forEach {
                val statusDto = arenaStatusDto(vedtakStatus(vedtakTom = it))
                assertThat(mapFaktaArena(statusDto, Stønadstype.BARNETILSYN).vedtakTom).isEqualTo(it)
            }
        }

        @Test
        fun `skal mappe vedtakTom hvis det er innen 6 plus 2 mnd for læremidler`() {
            val dato6mndSiden = LocalDate.now().minusMonths(8)

            assertThat(
                mapFaktaArena(
                    arenaStatusDto(vedtakStatus(vedtakTom = dato6mndSiden.minusDays(1))),
                    Stønadstype.LÆREMIDLER,
                ).vedtakTom,
            ).isNull()

            listOf(dato6mndSiden, dato6mndSiden.plusDays(1)).forEach {
                val statusDto = arenaStatusDto(vedtakStatus(vedtakTom = it))
                assertThat(mapFaktaArena(statusDto, Stønadstype.LÆREMIDLER).vedtakTom).isEqualTo(it)
            }
        }
    }
}
