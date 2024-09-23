package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerEndrePeriodeIForholdTilRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerNyPeriodeIForholdTilRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerSlettPeriodeIForholdTilRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.ikkeOppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.YearMonth

class VilkårRevurderFraValideringTest {
    val revurderFra = YearMonth.of(2024, 1)
    val behandling = saksbehandling(revurderFra = null, type = BehandlingType.REVURDERING)
    val behandlingMedRevurderFra = saksbehandling(revurderFra = revurderFra.atDay(1), type = BehandlingType.REVURDERING)

    @Nested
    inner class NyPeriode {

        @Test
        fun `kan legge inn periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                validerNyPeriodeIForholdTilRevurderFra(
                    behandling,
                    vilkår(fom = revurderFra.minusMonths(1)),
                )
                validerNyPeriodeIForholdTilRevurderFra(
                    behandling,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan legge inn periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerNyPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra),
                )
                validerNyPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan ikke legge inn ny periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val periode = vilkår(fom = revurderFra.minusMonths(1))
                validerNyPeriodeIForholdTilRevurderFra(behandlingMedRevurderFra, periode)
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
                    vilkår(fom = revurderFra.minusMonths(1)),
                )
                validerSlettPeriodeIForholdTilRevurderFra(
                    behandling,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan slette periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerSlettPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra),
                )
                validerSlettPeriodeIForholdTilRevurderFra(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan ikke slette periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val periode = vilkår(fom = revurderFra.minusMonths(1))
                validerSlettPeriodeIForholdTilRevurderFra(behandlingMedRevurderFra, periode)
            }.hasMessageContaining("Kan ikke slette periode")
        }
    }

    @Nested
    inner class OppdateringAvPeriode {

        @Test
        fun `kan oppdatere periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertDoesNotThrow {
                val eksisterendePeriode = vilkår(fom = revurderFra.minusMonths(2))
                endringUtenRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(delvilkårwrapper = DelvilkårWrapper(ikkeOppfylteDelvilkårPassBarn())),
                )
            }
            assertDoesNotThrow {
                val eksisterendePeriode = vilkår(fom = revurderFra.plusMonths(2))
                endringUtenRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(delvilkårwrapper = DelvilkårWrapper(ikkeOppfylteDelvilkårPassBarn())),
                )
            }
        }

        @Test
        fun `kan oppdatere tom hvis tom går tom siste dagen før revurder fra`() {
            val eksisterendePeriode = vilkår(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
            )
            assertDoesNotThrow {
                listOf(revurderFra.minusMonths(1), revurderFra, revurderFra.plusMonths(1)).forEach { nyttTom ->
                    endringMedRevurderFra(
                        eksisterendePeriode,
                        eksisterendePeriode.copy(tom = nyttTom.atEndOfMonth()),
                    )
                }
            }
        }

        @Test
        fun `kan ikke oppdatere tom til før dagen før revurder fra`() {
            val eksisterendePeriode = vilkår(
                fom = revurderFra.minusMonths(4),
                tom = revurderFra.plusMonths(1),
            )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(tom = revurderFra.minusMonths(2).atEndOfMonth()),
                )
            }.hasMessageContaining("Ugyldig endring på periode som begynner(01.09.2023) før revurderingsdato(01.01.2024)")
        }

        @Test
        fun `kan ikke oppdatere data på vilkår hvis det begynner før revurder-fra`() {
            val eksisterendePeriode = vilkår(
                fom = revurderFra.minusMonths(1),
                tom = revurderFra.plusMonths(1),
            )
            listOf<(Vilkår) -> Vilkår>(
                { it.copy(delvilkårwrapper = DelvilkårWrapper(ikkeOppfylteDelvilkårPassBarn())) },
                { it.copy(resultat = Vilkårsresultat.IKKE_OPPFYLT) },
                { it.copy(utgift = 200) },
            ).forEach { endreVilkårperiode ->
                assertThatThrownBy {
                    endringMedRevurderFra(
                        eksisterendePeriode,
                        endreVilkårperiode(eksisterendePeriode),
                    )
                }.hasMessageContaining("Ugyldig endring på periode som begynner(01.12.2023) før revurderingsdato(01.01.2024)")
            }
        }

        private fun endringUtenRevurderFra(
            eksisterendePeriode: Vilkår,
            oppdatertPeriode: Vilkår,
        ) {
            validerEndrePeriodeIForholdTilRevurderFra(
                behandling,
                eksisterendePeriode,
                oppdatertPeriode,
            )
        }

        private fun endringMedRevurderFra(
            eksisterendePeriode: Vilkår,
            oppdatertPeriode: Vilkår,
        ) {
            validerEndrePeriodeIForholdTilRevurderFra(
                behandlingMedRevurderFra,
                eksisterendePeriode,
                oppdatertPeriode,
            )
        }
    }

    private fun vilkår(
        fom: YearMonth,
        tom: YearMonth = fom,
        delvilkår: List<Delvilkår> = oppfylteDelvilkårPassBarn(),
        utgift: Int? = 1,
        resultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
    ) =
        vilkår(
            behandlingId = behandling.id,
            type = VilkårType.PASS_BARN,
            fom = fom.atDay(1),
            tom = tom.atEndOfMonth(),
            delvilkår = delvilkår,
            utgift = utgift,
            resultat = resultat,
        )
}
