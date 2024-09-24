package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerEndrePeriodeIForholdTilRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerNyPeriodeIForholdTilRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerSlettPeriodeIForholdTilRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.opprettVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class VilkårperiodeRevurderFraValideringTest {

    val revurderFra = LocalDate.of(2024, 1, 1)
    val behandling = saksbehandling(revurderFra = null, type = BehandlingType.REVURDERING)
    val behandlingMedRevurderFra = saksbehandling(revurderFra = revurderFra, type = BehandlingType.REVURDERING)

    @Nested
    inner class NyPeriode {

        @Test
        fun `kan legge inn periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                validerNyPeriodeIForholdTilRevurderFra(
                    behandling,
                    opprettVilkårperiodeMålgruppe(fom = revurderFra.minusDays(1)),
                )
                validerNyPeriodeIForholdTilRevurderFra(
                    behandling,
                    opprettVilkårperiodeMålgruppe(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan legge inn periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerNyPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    opprettVilkårperiodeMålgruppe(fom = revurderFra),
                )
                validerNyPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    opprettVilkårperiodeMålgruppe(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan ikke legge inn ny periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val vilkårperiode = opprettVilkårperiodeMålgruppe(fom = revurderFra.minusDays(1))
                validerNyPeriodeIForholdTilRevurderFra(behandlingMedRevurderFra, vilkårperiode)
            }.hasMessageContaining("Kan ikke opprette periode")
        }
    }

    @Nested
    inner class SlettPeriode {

        @Test
        fun `kan slette periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                validerSlettPeriodeIForholdTilRevurderFra(
                    behandling,
                    målgruppe(fom = revurderFra.minusDays(1)),
                )
                validerSlettPeriodeIForholdTilRevurderFra(
                    behandling,
                    målgruppe(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan slette periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerSlettPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    målgruppe(fom = revurderFra),
                )
                validerSlettPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    målgruppe(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan ikke slette periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val vilkårperiode = målgruppe(fom = revurderFra.minusDays(1))
                validerSlettPeriodeIForholdTilRevurderFra(behandlingMedRevurderFra, vilkårperiode)
            }.hasMessageContaining("Kan ikke slette periode")
        }
    }

    @Nested
    inner class OppdateringAvPeriode {

        @Test
        fun `kan oppdatere periode dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                val eksisterendeVilkårperiode = aktivitet(fom = revurderFra.minusDays(2), aktivitetsdager = 1)
                endringUtenRevurderFra(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.copy(aktivitetsdager = 2),
                )
            }
            assertDoesNotThrow {
                val eksisterendeVilkårperiode = aktivitet(fom = revurderFra.plusDays(1), aktivitetsdager = 1)
                endringUtenRevurderFra(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.copy(aktivitetsdager = 2),
                )
            }
        }

        @Test
        fun `kan oppdatere periodens tom-dato til og med dagen før revurder fra`() {
            val eksisterendeVilkårperiode = aktivitet(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
            )
            assertDoesNotThrow {
                listOf(revurderFra.minusDays(1), revurderFra, revurderFra.plusDays(1)).forEach { nyttTom ->
                    endringMedRevurderFra(
                        eksisterendeVilkårperiode,
                        eksisterendeVilkårperiode.copy(tom = nyttTom),
                    )
                }
            }
        }

        @Test
        fun `kan ikke oppdatere tom til 2 dager før revurder fra, då fjerner man data som gjelder dagen før revurderingsdatoet`() {
            val eksisterendeVilkårperiode = aktivitet(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
            )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.copy(tom = revurderFra.minusDays(2)),
                )
            }.hasMessageContaining("Ugyldig endring på periode som begynner(01.12.2023) før revurderingsdato(01.01.2024)")
        }

        @Test
        fun `kan ikke oppdatere data på periode hvis det begynner før revurder-fra`() {
            val eksisterendeVilkårperiode = aktivitet(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
                aktivitetsdager = 3,
                delvilkår = delvilkårAktivitet(lønnet = vurdering(SvarJaNei.NEI)),
                resultat = ResultatVilkårperiode.OPPFYLT,
            )
            listOf<(Vilkårperiode) -> Vilkårperiode>(
                { it.copy(aktivitetsdager = 2) },
                { it.copy(resultat = ResultatVilkårperiode.IKKE_OPPFYLT) },
                { it.copy(delvilkår = delvilkårAktivitet(lønnet = vurdering(SvarJaNei.JA))) },
            ).forEach { endreVilkårperiode ->
                assertThatThrownBy {
                    endringMedRevurderFra(
                        eksisterendeVilkårperiode,
                        endreVilkårperiode(eksisterendeVilkårperiode),
                    )
                }.hasMessageContaining("Ugyldig endring på periode som begynner(01.12.2023) før revurderingsdato(01.01.2024)")
            }
        }

        private fun endringUtenRevurderFra(
            eksisterendeVilkårperiode: Vilkårperiode,
            oppdatertVilkårperiode: Vilkårperiode,
        ) {
            validerEndrePeriodeIForholdTilRevurderFra(
                behandling,
                eksisterendeVilkårperiode,
                oppdatertVilkårperiode,
            )
        }

        private fun endringMedRevurderFra(
            eksisterendeVilkårperiode: Vilkårperiode,
            oppdatertVilkårperiode: Vilkårperiode,
        ) {
            validerEndrePeriodeIForholdTilRevurderFra(
                behandlingMedRevurderFra,
                eksisterendeVilkårperiode,
                oppdatertVilkårperiode,
            )
        }
    }
}
