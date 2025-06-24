package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseServiceTest {
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val tilkjentYtelseService =
        TilkjentYtelseService(
            tilkjentYtelseRepository,
        )

    private val fagsak = fagsak(setOf(PersonIdent("321")))
    private val behandling = behandling(fagsak = fagsak)

    @Nested
    inner class HarLøpendeUtbetaling {
        @Test
        internal fun `skal returnere true hvis det finnes andel med sluttdato etter idag`() {
            val andelTilkjentYtelse =
                andelTilkjentYtelse(
                    kildeBehandlingId = BehandlingId.random(),
                    beløp = 1,
                    fom = LocalDate.now().plusDays(1).datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    tom = LocalDate.now().plusDays(1).datoEllerNesteMandagHvisLørdagEllerSøndag(),
                )
            val tilkjentYtelse =
                tilkjentYtelse(behandling.id)
                    .copy(andelerTilkjentYtelse = setOf(andelTilkjentYtelse))
            every { tilkjentYtelseRepository.findByBehandlingId(any()) } returns tilkjentYtelse
            assertThat(tilkjentYtelseService.harLøpendeUtbetaling(behandling.id)).isTrue
        }

        @Test
        internal fun `skal returnere false hvis det finnes andel med sluttdato før idag`() {
            val andelTilkjentYtelse =
                andelTilkjentYtelse(
                    kildeBehandlingId = BehandlingId.random(),
                    beløp = 1,
                    fom = LocalDate.now().minusDays(10).datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    tom = LocalDate.now().minusDays(10).datoEllerNesteMandagHvisLørdagEllerSøndag(),
                )
            val tilkjentYtelse =
                tilkjentYtelse(behandling.id)
                    .copy(andelerTilkjentYtelse = setOf(andelTilkjentYtelse))
            every { tilkjentYtelseRepository.findByBehandlingId(any()) } returns tilkjentYtelse
            assertThat(tilkjentYtelseService.harLøpendeUtbetaling(behandling.id)).isFalse
        }

        @Test
        internal fun `skal returnere false hvis det ikke finnes noen andel`() {
            every { tilkjentYtelseRepository.findByBehandlingId(any()) } returns null
            assertThat(tilkjentYtelseService.harLøpendeUtbetaling(behandling.id)).isFalse
        }
    }
}
