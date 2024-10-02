package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.arenaStatusDto
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.vedtakStatus
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagArenaMapper.mapFaktaArena
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GrunnlagArenaMapperTest {

    @Nested
    inner class VedtakTom {

        val behandling = saksbehandling(opprettetTid = LocalDateTime.now())
        val dato12mndsiden = LocalDate.now().minusYears(1)

        @Test
        fun `skal mappe vedtakTom fra vedtakStatus hvis det er innen 12mnd`() {
            listOf(LocalDate.now(), null, dato12mndsiden, dato12mndsiden.plusDays(1)).forEach {
                assertThat(mapFaktaArena(arenaStatusDto(vedtakStatus(vedtakTom = it)), behandling).vedtakTom)
                    .isEqualTo(it)
            }
        }

        @Test
        fun `skal ikke mappe data hvis datoet er før 12mnd siden`() {
            val faktaArena =
                mapFaktaArena(arenaStatusDto(vedtakStatus(vedtakTom = dato12mndsiden.minusDays(1))), behandling)
            assertThat(faktaArena.vedtakTom).isNull()
        }
    }
}
