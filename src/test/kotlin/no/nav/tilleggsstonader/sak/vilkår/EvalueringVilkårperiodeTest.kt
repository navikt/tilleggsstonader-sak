package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.EvalueringVilkårperiode.evaulerVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerAktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EvalueringVilkårperiodeTest {

    @Nested
    inner class EvaluerMålgruppe {

        @Test
        fun `utled resultat for målgruppe`() {
            assertThat(utledResultatMålgruppe(SvarJaNei.JA)).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(utledResultatMålgruppe(SvarJaNei.JA_IMPLISITT)).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(utledResultatMålgruppe(SvarJaNei.NEI)).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(utledResultatMålgruppe(SvarJaNei.IKKE_VURDERT))
                .isEqualTo(ResultatVilkårperiode.IKKE_TATT_STILLING_TIL)
        }

        private fun utledResultatMålgruppe(medlemskap: SvarJaNei) = evaulerVilkårperiode(DetaljerMålgruppe(medlemskap))
    }

    @Nested
    inner class EvaluerAktivitet {

        @Test
        fun `ja implicitt er ikke et gyldig svar for lønnet`() {
            assertThatThrownBy {
                utledResultatAktivitet(
                    lønnet = SvarJaNei.JA_IMPLISITT,
                    mottarSykepenger = SvarJaNei.JA,
                )
            }.hasMessageContaining("Ikke gyldig svar for lønnet")
        }

        @Test
        fun `ja implicitt er ikke et gyldig svar for mottar sykepenger`() {
            assertThatThrownBy {
                utledResultatAktivitet(
                    lønnet = SvarJaNei.JA,
                    mottarSykepenger = SvarJaNei.JA_IMPLISITT,
                )
            }.hasMessageContaining("Ikke gyldig svar for mottarSykepenger")
        }

        @Test
        fun `hvis en ikke er vurdert stilling så er resultatet ikke tatt stilling til`() {
            val gyldigeSvarForAktivitet = SvarJaNei.entries
                .filter { it != SvarJaNei.JA_IMPLISITT } // ikke gyldig

            gyldigeSvarForAktivitet.forEach {
                assertThat(
                    utledResultatAktivitet(
                        lønnet = SvarJaNei.IKKE_VURDERT,
                        mottarSykepenger = it,
                    ),
                ).isEqualTo(ResultatVilkårperiode.IKKE_TATT_STILLING_TIL)

                assertThat(
                    utledResultatAktivitet(
                        lønnet = it,
                        mottarSykepenger = SvarJaNei.IKKE_VURDERT,
                    ),
                ).isEqualTo(ResultatVilkårperiode.IKKE_TATT_STILLING_TIL)
            }
        }

        @Test
        fun `hvis en ikke er oppfylt så er resultatet ikke oppfylt`() {
            val gyldigeSvarForAktivitet = SvarJaNei.entries
                .filter { it != SvarJaNei.JA_IMPLISITT } // ikke gyldig
                .filter { it != SvarJaNei.IKKE_VURDERT } // allerede testet i forrige test
            gyldigeSvarForAktivitet.forEach {
                assertThat(
                    utledResultatAktivitet(
                        lønnet = SvarJaNei.JA,
                        mottarSykepenger = it,
                    ),
                ).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

                assertThat(
                    utledResultatAktivitet(
                        lønnet = it,
                        mottarSykepenger = SvarJaNei.JA,
                    ),
                ).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }

        @Test
        fun `hvis begge er oppfylt er resultatet oppfylt`() {
            val resultat = utledResultatAktivitet(
                lønnet = SvarJaNei.NEI,
                mottarSykepenger = SvarJaNei.NEI,
            )
            assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        private fun utledResultatAktivitet(lønnet: SvarJaNei, mottarSykepenger: SvarJaNei) =
            evaulerVilkårperiode(DetaljerAktivitet(lønnet = lønnet, mottarSykepenger = mottarSykepenger))
    }
}
