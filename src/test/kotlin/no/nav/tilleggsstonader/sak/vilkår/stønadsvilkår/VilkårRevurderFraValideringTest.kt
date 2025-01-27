package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårRevurderFraValidering.validerSlettPeriodeRevurdering
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
                validerNyPeriodeRevurdering(
                    behandling,
                    vilkår(fom = revurderFra.minusMonths(1)),
                )
                validerNyPeriodeRevurdering(
                    behandling,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan legge inn periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerNyPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra),
                )
                validerNyPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan ikke legge inn ny periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val periode = vilkår(fom = revurderFra.minusMonths(1))
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
                    vilkår(fom = revurderFra.minusMonths(1)),
                )
                validerSlettPeriodeRevurdering(
                    behandling,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan slette periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra),
                )
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    vilkår(fom = revurderFra.plusMonths(1)),
                )
            }
        }

        @Test
        fun `kan ikke slette periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val periode = vilkår(fom = revurderFra.minusMonths(1))
                validerSlettPeriodeRevurdering(behandlingMedRevurderFra, periode)
            }.hasMessageContaining("Kan ikke slette periode")
        }
    }

    @Nested
    inner class OppdateringAvPeriode {
        @Test
        fun `kan oppdatere periode dersom revurder-fra ikke er satt`() {
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
        fun `kan oppdatere periodens tom-dato til og med dagen før revurder fra`() {
            val eksisterendePeriode =
                vilkår(
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
        fun `kan oppdatere begrunnelsen på en vurdering`() {
            val eksisterendePeriode =
                vilkår(
                    fom = revurderFra.minusMonths(1),
                    tom = revurderFra.plusMonths(1),
                )
            val oppdaterteVurderingerMedNyBegrunnelse =
                eksisterendePeriode.delvilkårwrapper.copy(
                    delvilkårsett =
                        eksisterendePeriode.delvilkårsett.map {
                            it.copy(vurderinger = it.vurderinger.map { it.copy(begrunnelse = "ny begrunnelse") })
                        },
                )
            assertDoesNotThrow {
                endringMedRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(delvilkårwrapper = oppdaterteVurderingerMedNyBegrunnelse),
                )
            }
        }

        @Test
        fun `kan ikke oppdatere tom til 2 dager før revurder fra, då fjerner man data som gjelder dagen før revurderingsdatoet`() {
            val eksisterendePeriode =
                vilkår(
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
        fun `kan ikke oppdatere data på periode hvis det begynner før revurder-fra`() {
            val eksisterendePeriode =
                vilkår(
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

        @Test
        fun `kan ikke endre fom til å begynne før revurderFra`() {
            val eksisterendePeriode =
                vilkår(
                    fom = revurderFra,
                    tom = revurderFra.plusMonths(1),
                )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendePeriode,
                    eksisterendePeriode.copy(fom = revurderFra.minusMonths(1).atDay(1)),
                )
            }.hasMessageContaining("Kan ikke sette fom før revurderingsdato")
        }

        private fun endringUtenRevurderFra(
            eksisterendePeriode: Vilkår,
            oppdatertPeriode: Vilkår,
        ) {
            validerEndrePeriodeRevurdering(
                behandling,
                eksisterendePeriode,
                oppdatertPeriode,
            )
        }

        private fun endringMedRevurderFra(
            eksisterendePeriode: Vilkår,
            oppdatertPeriode: Vilkår,
        ) {
            validerEndrePeriodeRevurdering(
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
    ) = vilkår(
        behandlingId = behandling.id,
        type = VilkårType.PASS_BARN,
        fom = fom.atDay(1),
        tom = tom.atEndOfMonth(),
        delvilkår = delvilkår,
        utgift = utgift,
        resultat = resultat,
    )
}
