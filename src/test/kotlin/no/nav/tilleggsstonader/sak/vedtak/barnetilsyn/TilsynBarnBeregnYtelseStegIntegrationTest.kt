package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.barn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth
import java.util.UUID

class TilsynBarnBeregnYtelseStegIntegrationTest(
    @Autowired
    val steg: TilsynBarnBeregnYtelseSteg,
    @Autowired
    val repository: TilsynBarnVedtakRepository,
    @Autowired
    val barnRepository: BarnRepository,
    @Autowired
    val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : IntegrationTest() {

    val behandling = behandling()
    val saksbehandling = saksbehandling(behandling = behandling)
    val barn = BehandlingBarn(behandlingId = behandling.id, ident = "123")

    val januar = YearMonth.of(2023, 1)
    val februar = YearMonth.of(2023, 2)
    val mars = YearMonth.of(2023, 3)
    val april = YearMonth.of(2023, 4)
    val utgift = 100

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        barnRepository.insert(barn)
    }

    @Nested
    inner class Innvilgelse {

        @Test
        fun `skal lagre vedtak`() {
            val vedtakDto = innvilgelseDto(
                stønadsperioder = listOf(Stønadsperiode(januar.atDay(1), januar.atEndOfMonth())),
                utgifter = mapOf(barn(barn.id, Utgift(januar, januar, 100))),
            )
            steg.utførSteg(saksbehandling, vedtakDto)

            val vedtak = repository.findByIdOrThrow(saksbehandling.id)

            assertThat(vedtak.behandlingId).isEqualTo(saksbehandling.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.INNVILGET)
            assertThat(vedtak.vedtak).isEqualTo(
                VedtaksdataTilsynBarn(
                    stønadsperioder = vedtakDto.stønadsperioder,
                    utgifter = vedtakDto.utgifter,
                ),
            )
        }

        @Test
        fun `skal lagre andeler for hver stønadsperiode, splittede per måned`() {
            val stønadsperiode1 = Stønadsperiode(januar.atDay(2), januar.atDay(6))
            val stønadsperiode2 = Stønadsperiode(januar.atDay(10), januar.atDay(11))
            val stønadsperiode3 = Stønadsperiode(januar.atDay(24), februar.atDay(3))
            val stønadsperiode4 = Stønadsperiode(februar.atDay(28), april.atDay(3))
            val vedtakDto = innvilgelseDto(
                stønadsperioder = listOf(stønadsperiode1, stønadsperiode2, stønadsperiode3, stønadsperiode4),
                mapOf(
                    barn(
                        barn.id,
                        Utgift(januar, februar, 100),
                        Utgift(mars, april, 200),
                    ),
                ),
            )
            steg.utførSteg(saksbehandling, vedtakDto)

            val dagsatsForUtgift100 = 3
            val dagsatsForUtgift200 = 6

            val forventedeAndeler = listOf(
                Pair(stønadsperiode1, dagsatsForUtgift100),
                Pair(stønadsperiode2, dagsatsForUtgift100),
                Pair(stønadsperiode3.copy(tom = januar.atEndOfMonth()), dagsatsForUtgift100),
                Pair(stønadsperiode3.copy(fom = februar.atDay(1)), dagsatsForUtgift100),
                Pair(stønadsperiode4.copy(tom = februar.atEndOfMonth()), dagsatsForUtgift100),
                Pair(Stønadsperiode(fom = mars.atDay(1), tom = mars.atEndOfMonth()), dagsatsForUtgift200),
                Pair(stønadsperiode4.copy(fom = april.atDay(1)), dagsatsForUtgift200),
            ).map {
                AndelTilkjentYtelse(
                    stønadFom = it.first.fom,
                    stønadTom = it.first.tom,
                    beløp = it.second,
                    kildeBehandlingId = behandling.id,
                )
            }
            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("sporbar")
                .containsExactlyElementsOf(forventedeAndeler)
        }
    }

    @Nested
    inner class ValideringInnvilgelse {
        @Test
        fun `skal validere at barn finnes på behandlingen`() {
            val vedtak = innvilgelseDto(
                stønadsperioder = listOf(Stønadsperiode(januar.atDay(1), januar.atEndOfMonth())),
                utgifter = mapOf(barn(UUID.randomUUID(), Utgift(januar, januar, utgift))),
            )
            assertThatThrownBy {
                steg.utførSteg(
                    saksbehandling,
                    vedtak,
                )
            }.hasMessageContaining("Det finnes utgifter på barn som ikke finnes på behandlingen")
        }
    }
}
