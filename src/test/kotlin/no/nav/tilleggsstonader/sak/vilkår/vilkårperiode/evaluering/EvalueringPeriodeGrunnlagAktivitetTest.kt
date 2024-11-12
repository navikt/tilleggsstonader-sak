package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringAktivitet.utledResultat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EvalueringPeriodeGrunnlagAktivitetTest {

    @Nested
    inner class Tiltak {

        @Test
        fun `hvis tiltak ikke er lønnet skal resultatet være oppfylt`() {
            val resultat = utledResultat(
                AktivitetType.TILTAK,
                delvilkårAktivitetDto(lønnet = SvarJaNei.NEI),
            )
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            assertThat(resultat.lønnet.svar).isEqualTo(SvarJaNei.NEI)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @Test
        fun `hvis tiltak er lønnet skal resultat være ikke oppfylt`() {
            val resultat = utledResultat(
                AktivitetType.TILTAK,
                delvilkårAktivitetDto(lønnet = SvarJaNei.JA),
            )
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            assertThat(resultat.lønnet.svar).isEqualTo(SvarJaNei.JA)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `hvis svar på lønnet mangler blir resultatet IKKE_VURDERT`() {
            val resultat = utledResultat(
                AktivitetType.TILTAK,
                delvilkårAktivitetDto(lønnet = null),
            )
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
            assertThat(resultat.lønnet.svar).isNull()

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }
    }

    @Nested
    inner class UtdanningEllerReellArbeidssøker {
        @ReellArbeidssøkerEllerUtdanningParameterizedTest
        fun `skal bli oppfylt uten svar på lønnet `(type: AktivitetType) {
            val delvilkår = delvilkårAktivitetDto()
            val resultat = utledResultat(type, delvilkår)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.lønnet.svar).isEqualTo(null)
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_AKTUELT)
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

        @Test
        fun `ja implisitt er ikke et gyldig svar for lønnet`() {
            assertThatThrownBy {
                utledResultat(
                    type = AktivitetType.TILTAK,
                    delvilkår = DelvilkårAktivitetDto(
                        lønnet = VurderingDto(SvarJaNei.JA_IMPLISITT),
                    ),
                )
            }.hasMessageContaining("Svar=JA_IMPLISITT er ikke gyldig svar for lønnet")
        }
    }

    @Nested
    inner class IngenPeriodeGrunnlagAktivitet {

        @Test
        fun `Ingen aktivitet skal mappes til ikke oppfylt`() {
            val resultat = utledResultat(
                AktivitetType.INGEN_AKTIVITET,
                delvilkårAktivitetDto(),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(resultat.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_AKTUELT)
            assertThat(resultat.lønnet.svar).isNull()
        }
    }

    private fun delvilkårAktivitetDto(
        lønnet: SvarJaNei? = null,
    ) = DelvilkårAktivitetDto(lønnet = VurderingDto(lønnet))

    private val ResultatEvaluering.lønnet: Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).lønnet
}

@ParameterizedTest
@EnumSource(
    value = AktivitetType::class,
    names = ["TILTAK", "INGEN_AKTIVITET"],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class ReellArbeidssøkerEllerUtdanningParameterizedTest
