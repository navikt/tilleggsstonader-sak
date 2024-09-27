package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class StønadsperiodeRevurderFraValideringTest {

    val revurderFra = LocalDate.of(2024, 1, 1)
    val behandling = saksbehandling(revurderFra = null, type = BehandlingType.REVURDERING)
    val behandlingMedRevurderFra = saksbehandling(revurderFra = revurderFra, type = BehandlingType.REVURDERING)

    @Nested
    inner class NyPeriode {

        @Test
        fun `kan legge inn periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                validerNyPeriodeRevurdering(
                    behandling,
                    stønadsperiode(fom = revurderFra.minusDays(1)),
                )
                validerNyPeriodeRevurdering(
                    behandling,
                    stønadsperiode(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan legge inn periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerNyPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    stønadsperiode(fom = revurderFra),
                )
                validerNyPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    stønadsperiode(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan ikke legge inn ny periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val periode = stønadsperiode(fom = revurderFra.minusDays(1))
                validerNyPeriodeRevurdering(behandlingMedRevurderFra, periode)
            }.hasMessageContaining("Kan ikke opprette periode")
        }
    }

    @Nested
    inner class SlettPeriode {

        @Test
        fun `kan slette periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                validerSlettPeriodeRevurdering(
                    behandling,
                    stønadsperiode(fom = revurderFra.minusDays(1)),
                )
                validerSlettPeriodeRevurdering(
                    behandling,
                    stønadsperiode(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan slette periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    stønadsperiode(fom = revurderFra),
                )
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    stønadsperiode(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan ikke slette periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val periode = stønadsperiode(fom = revurderFra.minusDays(1))
                validerSlettPeriodeRevurdering(behandlingMedRevurderFra, periode)
            }.hasMessageContaining("Kan ikke slette periode")
        }
    }

    @Nested
    inner class OppdateringAvPeriode {

        @Test
        fun `kan oppdatere periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                val eksisterendePeriode = stønadsperiode(fom = revurderFra.minusDays(2))
                endringUtenRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(fom = revurderFra.plusDays(2)),
                )
            }
            assertDoesNotThrow {
                val eksisterendePeriode = stønadsperiode(fom = revurderFra.plusDays(1))
                endringUtenRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(fom = revurderFra.plusDays(2)),
                )
            }
        }

        @Test
        fun `kan oppdatere tom hvis tom går tom siste dagen før revurder fra`() {
            val eksisterendePeriode = stønadsperiode(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
            )
            assertDoesNotThrow {
                listOf(revurderFra.minusDays(1), revurderFra, revurderFra.plusDays(1)).forEach { nyttTom ->
                    endringMedRevurderFra(
                        eksisterendePeriode,
                        eksisterendePeriode.copy(tom = nyttTom),
                    )
                }
            }
        }

        @Test
        fun `kan sende inn stønadsperiode uten endringer dersom den gjelder før revurderFra`() {
            val eksisterendePeriode = stønadsperiode(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.minusMonths(1),
            )
            assertDoesNotThrow {
                endringMedRevurderFra(eksisterendePeriode, eksisterendePeriode)
            }
        }

        @Test
        fun `kan ikke endre tom til før dagen før revurder fra`() {
            val eksisterendePeriode = stønadsperiode(
                fom = revurderFra.minusDays(2),
                tom = revurderFra.minusDays(1),
            )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(tom = revurderFra.minusDays(2)),
                )
            }.hasMessageContaining("Ugyldig endring på periode som")
        }

        @Test
        fun `kan ikke oppdatere tom til før dagen før revurder fra`() {
            val eksisterendePeriode = stønadsperiode(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
            )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(tom = revurderFra.minusDays(2)),
                )
            }.hasMessageContaining("Ugyldig endring på periode som begynner(01.12.2023) før revurderingsdato(01.01.2024)")
        }

        @Test
        fun `kan ikke oppdatere data på periode hvis det begynner før revurder-fra`() {
            val eksisterendePeriode = stønadsperiode(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )
            listOf<(Stønadsperiode) -> Stønadsperiode>(
                { it.copy(målgruppe = MålgruppeType.OVERGANGSSTØNAD) },
                { it.copy(aktivitet = AktivitetType.UTDANNING) },
            ).forEach { endreVilkårperiode ->
                assertThatThrownBy {
                    endringMedRevurderFra(
                        eksisterendePeriode,
                        endreVilkårperiode(eksisterendePeriode),
                    )
                }.hasMessageContaining("Ugyldig endring på periode som begynner(01.12.2023) før revurderingsdato(01.01.2024)")
            }
        }

        @Test
        fun `kan ikke oppdatere tom på vilkår hvis det begynner før revurder-fra`() {
            val eksisterendePeriode = stønadsperiode(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.minusMonths(1),
            )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(tom = eksisterendePeriode.tom.plusDays(1)),
                )
            }.hasMessageContaining("Ugyldig endring på periode")
        }

        @Test
        fun `kan ikke endre fom til å begynne før revurderFra`() {
            val eksisterendePeriode = stønadsperiode(
                fom = revurderFra,
                tom = revurderFra.plusMonths(1),
            )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(fom = revurderFra.minusDays(2)),
                )
            }.hasMessageContaining("Kan ikke sette fom før revurderingsdato")
        }

        private fun endringUtenRevurderFra(
            eksisterendePeriode: Stønadsperiode,
            oppdatertPeriode: Stønadsperiode,
        ) {
            validerEndrePeriodeRevurdering(
                behandling,
                eksisterendePeriode,
                oppdatertPeriode,
            )
        }

        private fun endringMedRevurderFra(
            eksisterendePeriode: Stønadsperiode,
            oppdatertPeriode: Stønadsperiode,
        ) {
            validerEndrePeriodeRevurdering(
                behandlingMedRevurderFra,
                eksisterendePeriode,
                oppdatertPeriode,
            )
        }
    }

    private fun stønadsperiode(
        fom: LocalDate,
        tom: LocalDate = fom.plusMonths(1),
        målgruppe: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) =
        stønadsperiode(
            behandlingId = BehandlingId.random(),
            fom = fom,
            tom = tom,
            målgruppe = målgruppe,
            aktivitet = aktivitet,
        )
}
