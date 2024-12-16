package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.medAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.medLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class VilkårperiodeRevurderFraValideringTest {

    val revurderFra = LocalDate.of(2024, 1, 1)
    val behandlingUtenRevurderFra = saksbehandling(revurderFra = null, type = BehandlingType.REVURDERING)
    val behandlingMedRevurderFra = saksbehandling(revurderFra = revurderFra, type = BehandlingType.REVURDERING)

    @Nested
    inner class NyPeriode {

        @Test
        fun `kan ikke gjøre endringer på periode dato dersom revurder-fra ikke er satt`() {
            assertThatThrownBy {
                validerNyPeriodeRevurdering(
                    behandling = behandlingUtenRevurderFra,
                    fom = revurderFra.minusDays(1),
                )
            }.hasMessageContaining("Revurder fra-dato må settes før du kan gjøre endringer på inngangvilkårene")
        }

        @Test
        fun `kan legge inn periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerNyPeriodeRevurdering(
                    behandling = behandlingMedRevurderFra,
                    fom = revurderFra,
                )
                validerNyPeriodeRevurdering(
                    behandling = behandlingMedRevurderFra,
                    fom = revurderFra.plusDays(1),
                )
            }
        }

        @Test
        fun `kan ikke legge inn ny periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                validerNyPeriodeRevurdering(behandling = behandlingMedRevurderFra, fom = revurderFra.minusDays(1))
            }.hasMessageContaining("Kan ikke opprette periode")
        }
    }

    @Nested
    inner class SlettPeriode {

        @Test
        fun `kan ikke slette periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertThatThrownBy {
                validerSlettPeriodeRevurdering(
                    behandlingUtenRevurderFra,
                    målgruppe(fom = revurderFra.plusDays(1)),
                )
            }.hasMessageContaining("Revurder fra-dato må settes før du kan gjøre endringer på inngangvilkårene")
        }

        @Test
        fun `kan slette periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    målgruppe(fom = revurderFra),
                )
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    målgruppe(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan ikke slette periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val vilkårperiode = målgruppe(fom = revurderFra.minusDays(1))
                validerSlettPeriodeRevurdering(behandlingMedRevurderFra, vilkårperiode)
            }.hasMessageContaining("Kan ikke slette periode")
        }
    }

    @Nested
    inner class OppdateringAvPeriode {

        @Test
        fun `kan oppdatere periode hvis revurder-fra er satt`() {
            assertDoesNotThrow {
                val eksisterendeVilkårperiode = aktivitet(
                    fom = revurderFra.plusDays(1),
                    faktaOgVurdering = faktaOgVurderingAktivitet(
                        aktivitetsdager = 1,
                    ),
                )
                endringMedRevurderFra(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.medAktivitetsdager(aktivitetsdager = 2),
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
                faktaOgVurdering = faktaOgVurderingAktivitet(
                    aktivitetsdager = 3,
                    lønnet = vurderingLønnet(SvarJaNei.NEI),
                ),
                resultat = ResultatVilkårperiode.OPPFYLT,
            )
            listOf<(Vilkårperiode) -> Vilkårperiode>(
                { it.medAktivitetsdager(aktivitetsdager = 2) },
                { it.copy(resultat = ResultatVilkårperiode.IKKE_OPPFYLT) },
                { it.copy(begrunnelse = "en begrunnelse").medLønnet(vurderingLønnet(SvarJaNei.JA)) },
            ).forEach { endreVilkårperiode ->
                assertThatThrownBy {
                    endringMedRevurderFra(
                        eksisterendeVilkårperiode,
                        endreVilkårperiode(eksisterendeVilkårperiode),
                    )
                }.hasMessageContaining("Ugyldig endring på periode som begynner(01.12.2023) før revurderingsdato(01.01.2024)")
            }
        }

        @Test
        fun `kan ikke endre fom til å begynne før revurderFra`() {
            val eksisterendeVilkårperiode = aktivitet(
                fom = revurderFra,
                tom = revurderFra.plusMonths(1),
            )
            assertThatThrownBy {
                endringMedRevurderFra(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.copy(fom = revurderFra.minusDays(2)),
                )
            }.hasMessageContaining("Kan ikke sette fom før revurderingsdato")
        }

        private fun endringMedRevurderFra(
            eksisterendeVilkårperiode: Vilkårperiode,
            oppdatertVilkårperiode: Vilkårperiode,
        ) {
            validerEndrePeriodeRevurdering(
                behandlingMedRevurderFra,
                eksisterendeVilkårperiode,
                oppdatertVilkårperiode,
            )
        }
    }
}
