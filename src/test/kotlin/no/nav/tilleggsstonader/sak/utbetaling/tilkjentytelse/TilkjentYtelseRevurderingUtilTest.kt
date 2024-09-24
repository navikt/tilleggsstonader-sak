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

    val revurdering = saksbehandling(revurderFra = LocalDate.now(), type = BehandlingType.REVURDERING)

    @Nested
    inner class ValiderNyeAndelerBegynnerEtterRevurderFra {

        @Test
        fun `skal validere ok hvis revurderFra ikke er satt`() {
            validerNyeAndelerBegynnerEtterRevurderFra(
                saksbehandling(),
                listOf(andelTilkjentYtelse(kildeBehandlingId = BehandlingId.random())),
            )
        }

        @Test
        fun `skal validere ok hvis andel begynner fra revurderingsdato eller etter`() {
            validerNyeAndelerBegynnerEtterRevurderFra(
                revurdering,
                listOf(
                    andelTilkjentYtelse(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)),
                    andelTilkjentYtelse(fom = LocalDate.now().plusDays(1), tom = LocalDate.now().plusDays(1)),
                ),
            )
        }

        @Test
        fun `skal kaste feil hvis andel begynner fra revurderingsdato eller etter`() {
            assertThatThrownBy {
                validerNyeAndelerBegynnerEtterRevurderFra(
                    revurdering,
                    listOf(
                        andelTilkjentYtelse(fom = LocalDate.now().minusDays(1), tom = LocalDate.now().plusDays(1)),
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

        @Test
        fun `skal gjenbrukte felter fra andeler som overlapper med revurderFra`() {
            val revurderFra = LocalDate.of(2024, 1, 10)
            val andel = iverksattAndel(
                fom = revurderFra.minusDays(7),
                tom = revurderFra.plusDays(2),
                type = TypeAndel.TILSYN_BARN_AAP,
            )
            val tilkjentYtelse = tilkjentYtelse(BehandlingId.random(), andel)

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(tilkjentYtelse, revurderFra)

            assertGjenbruktAndel(gjenbrukteAndeler, andel, nyTom = revurderFra.minusDays(1))
        }

        @Test
        fun `skal beholde andeler som slutter før revurderFra`() {
            val revurderFra = LocalDate.of(2024, 1, 10)
            val andel = iverksattAndel(
                fom = revurderFra.minusDays(2),
                tom = revurderFra.minusDays(2),
                type = TypeAndel.TILSYN_BARN_AAP,
            )
            val tilkjentYtelse = tilkjentYtelse(BehandlingId.random(), andel)

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(tilkjentYtelse, revurderFra)

            assertGjenbruktAndel(gjenbrukteAndeler, andel, nyTom = revurderFra.minusDays(2))
        }

        @Test
        fun `skal avkorte en andel som slutter på lik dato som revurderFra`() {
            val revurderFra = LocalDate.of(2024, 1, 10)
            val andel =
                iverksattAndel(fom = revurderFra.minusDays(2), tom = revurderFra, type = TypeAndel.TILSYN_BARN_AAP)
            val tilkjentYtelse = tilkjentYtelse(BehandlingId.random(), andel)

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(tilkjentYtelse, revurderFra)

            assertThat(gjenbrukteAndeler.single().tom).isEqualTo(revurderFra.minusDays(1))
        }

        @Test
        fun `skal filtrere vekk andeler som begynner etter revurderFra`() {
            val revurderFra = LocalDate.of(2024, 1, 8)
            val tilkjentYtelse = tilkjentYtelse(
                BehandlingId.random(),
                iverksattAndel(fom = revurderFra, tom = revurderFra, type = TypeAndel.TILSYN_BARN_AAP),
                iverksattAndel(fom = revurderFra.plusDays(1), tom = revurderFra.plusDays(1), type = TypeAndel.TILSYN_BARN_AAP),
            )

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(tilkjentYtelse, revurderFra)

            assertThat(gjenbrukteAndeler).isEmpty()
        }

        @Test
        fun `hvis man revurderer fra en mandag, så skal fredag som ny tom`() {
            val revurderFra = LocalDate.of(2024, 1, 8) // mandag
            val andel = iverksattAndel(
                fom = LocalDate.of(2024, 1, 2),
                tom = revurderFra.plusDays(2),
                type = TypeAndel.TILSYN_BARN_AAP,
            )
            val tilkjentYtelse = tilkjentYtelse(BehandlingId.random(), andel)

            val gjenbrukteAndeler = gjenbrukAndelerFraForrigeTilkjentYtelse(tilkjentYtelse, revurderFra)

            assertGjenbruktAndel(gjenbrukteAndeler, andel, nyTom = LocalDate.of(2024, 1, 5))
        }

        private fun assertGjenbruktAndel(
            gjenbrukteAndeler: List<AndelTilkjentYtelse>,
            andel: AndelTilkjentYtelse,
            nyTom: LocalDate,
        ) {
            with(gjenbrukteAndeler.single()) {
                assertThat(fom).isEqualTo(andel.fom)
                assertThat(tom).isEqualTo(nyTom)
                assertThat(type).isEqualTo(andel.type)
                assertThat(kildeBehandlingId).isEqualTo(andel.kildeBehandlingId)
                assertThat(beløp).isEqualTo(andel.beløp)
                assertThat(satstype).isEqualTo(andel.satstype)
                assertThat(iverksetting).isNull()
                assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            }
        }

        fun iverksattAndel(
            fom: LocalDate,
            tom: LocalDate,
            type: TypeAndel,
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
}
