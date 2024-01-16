package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.EvalueringVilkårperiode.evaulerVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårMålgruppeDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EvalueringVilkårperiodeTest {

    @Nested
    inner class EvaluerMålgruppe {

        @Test
        fun `utled resultat for målgruppe`() {
            DelvilkårMålgruppeDto(medlemskap = SvarJaNei.JA)
                .assertMappesTil(ResultatVilkårperiode.OPPFYLT, ResultatDelvilkårperiode.OPPFYLT)

            DelvilkårMålgruppeDto(medlemskap = SvarJaNei.JA_IMPLISITT)
                .assertMappesTil(ResultatVilkårperiode.OPPFYLT, ResultatDelvilkårperiode.OPPFYLT)

            DelvilkårMålgruppeDto(medlemskap = SvarJaNei.NEI)
                .assertMappesTil(ResultatVilkårperiode.IKKE_OPPFYLT, ResultatDelvilkårperiode.IKKE_OPPFYLT)

            DelvilkårMålgruppeDto(medlemskap = null)
                .assertMappesTil(ResultatVilkårperiode.IKKE_VURDERT, ResultatDelvilkårperiode.IKKE_VURDERT)
        }

        fun DelvilkårMålgruppeDto.assertMappesTil(
            resultatVilkårperiode: ResultatVilkårperiode,
            resultatMedlemskap: ResultatDelvilkårperiode,
        ) {
            val evaluering = evaulerVilkårperiode(this)
            assertThat(evaluering.resultat).isEqualTo(resultatVilkårperiode)
            assertThat((evaluering.delvilkår as DelvilkårMålgruppe).medlemskap.resultat).isEqualTo(resultatMedlemskap)
            assertThat((evaluering.delvilkår as DelvilkårMålgruppe).medlemskap.svar).isEqualTo(this.medlemskap)
        }
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
                        lønnet = null,
                        mottarSykepenger = it,
                    ).resultat,
                ).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)

                assertThat(
                    utledResultatAktivitet(
                        lønnet = it,
                        mottarSykepenger = null,
                    ).resultat,
                ).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
            }
        }

        @Test
        fun `hvis en ikke er oppfylt så er resultatet ikke oppfylt`() {
            val gyldigeSvarForAktivitet = SvarJaNei.entries
                .filter { it != SvarJaNei.JA_IMPLISITT }
            gyldigeSvarForAktivitet.forEach {
                assertThat(
                    utledResultatAktivitet(
                        lønnet = SvarJaNei.JA,
                        mottarSykepenger = it,
                    ).resultat,
                ).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

                assertThat(
                    utledResultatAktivitet(
                        lønnet = it,
                        mottarSykepenger = SvarJaNei.JA,
                    ).resultat,
                ).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }

        @Test
        fun `hvis begge er oppfylt er resultatet oppfylt`() {
            val resultatEvaluering = utledResultatAktivitet(
                lønnet = SvarJaNei.NEI,
                mottarSykepenger = SvarJaNei.NEI,
            )
            assertThat(resultatEvaluering.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @Test
        fun `skal vurdere hvert delvilkår for seg`() {
            with(
                utledResultatAktivitet(
                    lønnet = SvarJaNei.NEI,
                    mottarSykepenger = null,
                ).delvilkår as DelvilkårAktivitet,
            ) {
                assertThat(lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                assertThat(lønnet.svar).isEqualTo(SvarJaNei.NEI)

                assertThat(mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
                assertThat(mottarSykepenger.svar).isNull()
            }

            with(
                utledResultatAktivitet(
                    lønnet = null,
                    mottarSykepenger = SvarJaNei.JA,
                ).delvilkår as DelvilkårAktivitet,
            ) {
                assertThat(lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
                assertThat(lønnet.svar).isEqualTo(null)

                assertThat(mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
                assertThat(mottarSykepenger.svar).isEqualTo(SvarJaNei.JA)
            }
        }

        private fun utledResultatAktivitet(lønnet: SvarJaNei?, mottarSykepenger: SvarJaNei?) =
            evaulerVilkårperiode(DelvilkårAktivitetDto(lønnet = lønnet, mottarSykepenger = mottarSykepenger))
    }
}
