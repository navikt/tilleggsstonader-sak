package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseRevurderingUtil.gjenbrukAndelerFraForrigeTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseRevurderingUtil.validerNyeAndelerBegynnerEtterRevurderFra
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilkjentYtelseRevurderingUtilTest {

    val revurderFra = LocalDate.of(2024, 8, 6)

    val revurdering = saksbehandling(revurderFra = revurderFra, type = BehandlingType.REVURDERING)

    @Nested
    inner class ValiderNyeAndelerBegynnerEtterRevurderFra {

        @Test
        fun `skal validere ok hvis revurderFra ikke er satt`() {
            validerNyeAndelerBegynnerEtterRevurderFra(
                saksbehandling(),
                listOf(
                    iverksattAndel(fom = revurderFra.minusDays(5)),
                    iverksattAndel(fom = revurderFra),
                    iverksattAndel(fom = revurderFra.plusDays(7)),
                ),
            )
        }

        @Test
        fun `skal validere ok hvis andel begynner fra revurderingsdato eller etter`() {
            validerNyeAndelerBegynnerEtterRevurderFra(
                revurdering,
                listOf(
                    iverksattAndel(fom = revurderFra),
                    iverksattAndel(fom = revurderFra.plusDays(1)),
                ),
            )
        }

        @Test
        fun `skal kaste feil hvis andel begynner før revurderingsdato`() {
            assertThatThrownBy {
                validerNyeAndelerBegynnerEtterRevurderFra(
                    revurdering,
                    listOf(
                        iverksattAndel(fom = revurderFra.minusDays(1)),
                    ),
                )
            }.hasMessageContaining("Kan ikke opprette andeler som begynner før revurderFra")
        }
    }

    /**
     * Problemet med gjenbruk er at dagsats-perioder kun løper 1 dag. Så det med "gjenbruk" virker ikke veldig bra.
     */
    @Nested
    inner class GjenbrukAndelerFraForrigeTilkjentYtelse {

        val behandling = saksbehandling()

        @Test
        fun `skal gjenbrukte andeler som slutter før måneden for revurderFra`() {
            val revurderFra = LocalDate.of(2024, 1, 10)
            val andel = iverksattAndel(
                fom = LocalDate.of(2023, 12, 1),
                type = TypeAndel.TILSYN_BARN_AAP,
            )
            val tilkjentYtelse = tilkjentYtelse(BehandlingId.random(), andel)

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(behandling, tilkjentYtelse, revurderFra)

            assertGjenbruktAndel(gjenbrukteAndeler, andel)
        }

        @Test
        fun `skal ikke beholde andeler som er i den samme måneden som revurderFra, selv om de gjelder før revurderFra`() {
            val revurderFra = LocalDate.of(2024, 1, 10)
            val andel = iverksattAndel(
                fom = revurderFra.minusDays(2),
                type = TypeAndel.TILSYN_BARN_AAP,
            )
            val tilkjentYtelse = tilkjentYtelse(BehandlingId.random(), andel)

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(behandling, tilkjentYtelse, revurderFra)

            assertThat(gjenbrukteAndeler).isEmpty()
        }

        @Test
        fun `skal filtrere vekk andeler som begynner etter revurderFra`() {
            val revurderFra = LocalDate.of(2024, 1, 8)
            val tilkjentYtelse = tilkjentYtelse(
                BehandlingId.random(),
                iverksattAndel(fom = revurderFra, type = TypeAndel.TILSYN_BARN_AAP),
                iverksattAndel(fom = revurderFra.plusDays(1), type = TypeAndel.TILSYN_BARN_AAP),
                iverksattAndel(fom = LocalDate.of(2024, 2, 6), type = TypeAndel.TILSYN_BARN_AAP),
            )

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(behandling, tilkjentYtelse, revurderFra)

            assertThat(gjenbrukteAndeler).isEmpty()
        }

        @Test
        fun `skal filtrere vekk nullperioder av type ugyldig då de ikke skal iverksettes`() {
            val revurderFra = LocalDate.of(2025, 1, 8) // mandag
            val andel = iverksattAndel(
                fom = LocalDate.of(2024, 1, 2),
                type = TypeAndel.UGYLDIG,
            )
            val tilkjentYtelse = tilkjentYtelse(BehandlingId.random(), andel)

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(behandling, tilkjentYtelse, revurderFra)

            assertThat(gjenbrukteAndeler).isEmpty()
        }

        private fun assertGjenbruktAndel(
            gjenbrukteAndeler: List<AndelTilkjentYtelse>,
            andel: AndelTilkjentYtelse,
        ) {
            with(gjenbrukteAndeler.single()) {
                assertThat(fom).isEqualTo(fom)
                assertThat(tom).isEqualTo(tom)
                assertThat(type).isEqualTo(andel.type)
                assertThat(kildeBehandlingId).isEqualTo(andel.kildeBehandlingId)
                assertThat(beløp).isEqualTo(andel.beløp)
                assertThat(satstype).isEqualTo(andel.satstype)
                assertThat(iverksetting).isNull()
                assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            }
        }
    }

    private fun iverksattAndel(
        fom: LocalDate,
        tom: LocalDate = fom,
        type: TypeAndel = TypeAndel.TILSYN_BARN_AAP,
    ) = andelTilkjentYtelse(
        fom = fom,
        tom = tom,
        type = type,
        kildeBehandlingId = BehandlingId.random(),
        beløp = 100,
        satstype = Satstype.DAG,
        iverksetting = Iverksetting(UUID.randomUUID(), LocalDateTime.now()),
        statusIverksetting = StatusIverksetting.SENDT,
    )
}
