package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringAktivitet.utledResultat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EvalueringAktivitetTest {

    @Nested
    inner class IkkeReelArbeidssøker {

        @IkkeReellArbeidssøkerParameterizedTest
        fun `hvis ikke lønnet og ikke mottar sykepenger så er resultatet oppfylt`(type: AktivitetType) {
            val resultat =
                utledResultat(type, delvilkårAktivitetDto(lønnet = SvarJaNei.NEI, mottarSykepenger = SvarJaNei.NEI))
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            assertThat(resultat.lønnet.svar).isEqualTo(SvarJaNei.NEI)

            assertThat(resultat.mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            assertThat(resultat.mottarSykepenger.svar).isEqualTo(SvarJaNei.NEI)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @IkkeReellArbeidssøkerParameterizedTest
        fun `skal vurdere lønnet alene`(type: AktivitetType) {
            val resultat = utledResultat(type, delvilkårAktivitetDto(lønnet = SvarJaNei.NEI, mottarSykepenger = null))
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            assertThat(resultat.lønnet.svar).isEqualTo(SvarJaNei.NEI)

            assertThat(resultat.mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
            assertThat(resultat.mottarSykepenger.svar).isNull()

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @IkkeReellArbeidssøkerParameterizedTest
        fun `skal vurdere mottar sykepenger alene`(type: AktivitetType) {
            val resultat = utledResultat(type, delvilkårAktivitetDto(lønnet = null, mottarSykepenger = SvarJaNei.JA))
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
            assertThat(resultat.lønnet.svar).isEqualTo(null)

            assertThat(resultat.mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            assertThat(resultat.mottarSykepenger.svar).isEqualTo(SvarJaNei.JA)
            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @IkkeReellArbeidssøkerParameterizedTest
        fun `hvis en ikke er oppfylt så er resultatet ikke oppfylt`(type: AktivitetType) {
            val gyldigeSvarForAktivitet = SvarJaNei.entries.filter { it != SvarJaNei.JA_IMPLISITT }
            gyldigeSvarForAktivitet.forEach {
                assertThat(
                    utledResultat(type, delvilkårAktivitetDto(lønnet = SvarJaNei.JA, mottarSykepenger = it)).resultat,
                ).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

                assertThat(
                    utledResultat(type, delvilkårAktivitetDto(lønnet = it, mottarSykepenger = SvarJaNei.JA)).resultat,
                ).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }
    }

    @Nested
    inner class ReelArbeidssøker {
        @Test
        fun `mottar sykepenger skal mappes til IKKE_OPPFYLT`() {
            val delvilkår = delvilkårAktivitetDto(mottarSykepenger = SvarJaNei.JA)
            val resultat = utledResultat(AktivitetType.REELL_ARBEIDSSØKER, delvilkår)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

            assertThat(resultat.lønnet.svar).isEqualTo(null)
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT) // TODO?

            assertThat(resultat.mottarSykepenger.svar).isEqualTo(SvarJaNei.JA)
            assertThat(resultat.mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `mottar ikke sykepenger skal mappes til OPPFYLT`() {
            val delvilkår = delvilkårAktivitetDto(mottarSykepenger = SvarJaNei.NEI)
            val resultat = utledResultat(AktivitetType.REELL_ARBEIDSSØKER, delvilkår)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.lønnet.svar).isEqualTo(null)
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT) // TODO?

            assertThat(resultat.mottarSykepenger.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(resultat.mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @ParameterizedTest
        @EnumSource(value = SvarJaNei::class)
        fun `svar på lønnet skal kaste feil`(svar: SvarJaNei) {
            assertThatThrownBy {
                utledResultat(AktivitetType.REELL_ARBEIDSSØKER, delvilkårAktivitetDto(lønnet = svar))
            }.hasMessageContaining("Ugyldig svar=$svar for lønnet")
        }
    }

    @Nested
    inner class JaImplisitt {
        @ParameterizedTest
        @EnumSource(value = AktivitetType::class, names = ["REEL_ARBEIDSSØKER"], mode = EnumSource.Mode.EXCLUDE)
        fun `ja implicitt er ikke et gyldig svar for lønnet`(type: AktivitetType) {
            assertThatThrownBy {
                utledResultat(
                    type = type,
                    delvilkår = DelvilkårAktivitetDto(
                        lønnet = SvarJaNei.JA_IMPLISITT,
                        mottarSykepenger = null,
                    ),
                )
            }.hasMessageContaining("Svar=JA_IMPLISITT er ikke gyldig svar for lønnet")
        }

        @ParameterizedTest
        @EnumSource(value = AktivitetType::class)
        fun `ja implicitt er ikke et gyldig svar for mottar sykepenger`(type: AktivitetType) {
            assertThatThrownBy {
                utledResultat(
                    type = type,
                    delvilkår = DelvilkårAktivitetDto(
                        lønnet = null,
                        mottarSykepenger = SvarJaNei.JA_IMPLISITT,
                    ),
                )
            }.hasMessageContaining("Svar=JA_IMPLISITT er ikke gyldig svar for mottarSykepenger")
        }
    }

    private fun delvilkårAktivitetDto(
        lønnet: SvarJaNei? = null,
        mottarSykepenger: SvarJaNei? = null,
    ) = DelvilkårAktivitetDto(lønnet = lønnet, mottarSykepenger = mottarSykepenger)

    private val ResultatEvaluering.lønnet: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).lønnet

    private val ResultatEvaluering.mottarSykepenger: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).mottarSykepenger
}

@ParameterizedTest
@EnumSource(
    value = AktivitetType::class,
    names = ["REELL_ARBEIDSSØKER"],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class IkkeReellArbeidssøkerParameterizedTest
