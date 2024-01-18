package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.medlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringMålgruppe.utledResultat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EvalueringMålgruppeTest {

    val svarJa = DelvilkårMålgruppeDto(VurderingDto(SvarJaNei.JA))
    val svarImplisitt = DelvilkårMålgruppeDto(VurderingDto(SvarJaNei.JA_IMPLISITT))
    val svarNei = DelvilkårMålgruppeDto(VurderingDto(SvarJaNei.NEI))
    val svarMangler = DelvilkårMålgruppeDto(VurderingDto(null))

    @Nested
    inner class Implisitt {
        @ImplisittParameterizedTest
        fun `mangler svar skal mappes til oppfylt`(type: MålgruppeType) {
            val resultat = utledResultat(type, svarMangler)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @ImplisittParameterizedTest
        fun `implisitt svar skal mappes til oppfylt`(type: MålgruppeType) {
            val resultat = utledResultat(type, svarImplisitt)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @ImplisittParameterizedTest
        fun `skal kaste feil hvis man svarer ja eller nei`(type: MålgruppeType) {
            assertThatThrownBy {
                utledResultat(type, svarJa)
            }.hasMessageContaining("Kan ikke evaluere svar=JA på medlemskap for type=$type")

            assertThatThrownBy {
                utledResultat(type, svarNei)
            }.hasMessageContaining("Kan ikke evaluere svar=NEI på medlemskap for type=$type")
        }
    }

    @Nested
    inner class IkkeImplisitt {
        @IkkeImplisittParameterizedTest
        fun `mangler svar skal mappes til IKKE_VURDERT`(type: MålgruppeType) {
            val resultat = utledResultat(type, svarMangler)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)

            assertThat(resultat.medlemskap.svar).isNull()
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
        }

        @IkkeImplisittParameterizedTest
        fun `ja skal mappes til OPPFYLT`(type: MålgruppeType) {
            val resultat = utledResultat(type, svarJa)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.JA)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @IkkeImplisittParameterizedTest
        fun `nei skal mappes til IKKE_OPPFYLT`(type: MålgruppeType) {
            val resultat = utledResultat(type, svarNei)

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @IkkeImplisittParameterizedTest
        fun `implisitt svar skal kaste feil`(type: MålgruppeType) {
            assertThatThrownBy {
                utledResultat(type, svarImplisitt)
            }.hasMessageContaining("Ugyldig svar=JA_IMPLISITT")
        }
    }
}

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = ["NEDSATT_ARBEIDSEVNE", "OMSTILLINGSSTØNAD", "DAGPENGER"],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class ImplisittParameterizedTest

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = ["AAP", "UFØRETRYGD", "OVERGANGSSTØNAD"],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class IkkeImplisittParameterizedTest
