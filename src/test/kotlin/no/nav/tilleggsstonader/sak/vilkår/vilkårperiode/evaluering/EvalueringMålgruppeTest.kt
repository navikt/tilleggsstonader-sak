package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.dekketAvAnnetRegelverk
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

    val jaVurdering = VurderingDto(SvarJaNei.JA)
    val implisittVurdering = VurderingDto(SvarJaNei.JA_IMPLISITT)
    val neiVurdering = VurderingDto(SvarJaNei.NEI, "begrunnelse")
    val svarManglerVurdering = VurderingDto(null)

    @Nested
    inner class ImplisittMedlemskap {
        @ImplisittParameterizedTest
        fun `mangler svar skal mappes til oppfylt`(type: MålgruppeType) {
            val resultat = utledResultat(
                type,
                delvilkårMålgruppeDto(
                    medlemskap = svarManglerVurdering,
                    dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                ),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @ImplisittParameterizedTest
        fun `implisitt svar skal mappes til oppfylt`(type: MålgruppeType) {
            val resultat = utledResultat(
                type,
                delvilkårMålgruppeDto(
                    medlemskap = implisittVurdering,
                    dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                ),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @ImplisittParameterizedTest
        fun `skal kaste feil hvis man svarer ja eller nei`(type: MålgruppeType) {
            assertThatThrownBy {
                utledResultat(
                    type,
                    delvilkårMålgruppeDto(
                        medlemskap = jaVurdering,
                        dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                    ),
                )
            }.hasMessageContaining("Kan ikke evaluere svar=JA på medlemskap for type=$type")

            assertThatThrownBy {
                utledResultat(
                    type,
                    delvilkårMålgruppeDto(
                        medlemskap = neiVurdering,
                        dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                    ),
                )
            }.hasMessageContaining("Kan ikke evaluere svar=NEI på medlemskap for type=$type")
        }
    }

    @Nested
    inner class IkkeImplisittMedlemskap {
        @IkkeImplisittParameterizedTest
        fun `mangler svar skal mappes til IKKE_VURDERT`(type: MålgruppeType) {
            val resultat = utledResultat(
                type,
                delvilkårMålgruppeDto(
                    medlemskap = svarManglerVurdering,
                    dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                ),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)

            assertThat(resultat.medlemskap.svar).isNull()
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
        }

        @IkkeImplisittParameterizedTest
        fun `ja skal mappes til OPPFYLT`(type: MålgruppeType) {
            val resultat = utledResultat(
                type,
                delvilkårMålgruppeDto(
                    medlemskap = jaVurdering,
                    dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                ),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.JA)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @IkkeImplisittParameterizedTest
        fun `nei skal mappes til IKKE_OPPFYLT`(type: MålgruppeType) {
            val resultat = utledResultat(
                type,
                delvilkårMålgruppeDto(
                    medlemskap = neiVurdering,
                    dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                ),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

            assertThat(resultat.medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @IkkeImplisittParameterizedTest
        fun `implisitt svar skal kaste feil`(type: MålgruppeType) {
            assertThatThrownBy {
                utledResultat(
                    type,
                    delvilkårMålgruppeDto(
                        medlemskap = implisittVurdering,
                        dekketAvAnnetRegelverk = oppfyltDekketAvAnnetRegelverk(type),
                    ),
                )
            }.hasMessageContaining("Ugyldig svar=JA_IMPLISITT")
        }
    }

    private fun delvilkårMålgruppeDto(
        medlemskap: VurderingDto,
        dekketAvAnnetRegelverk: VurderingDto?,
    ): DelvilkårMålgruppeDto {
        return DelvilkårMålgruppeDto(
            medlemskap = medlemskap,
            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
        )
    }

    private fun oppfyltDekketAvAnnetRegelverk(type: MålgruppeType): VurderingDto? {
        if (!type.gjelderNedsattArbeidsevne()) return null

        return VurderingDto(svar = SvarJaNei.NEI)
    }
}

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = ["NEDSATT_ARBEIDSEVNE", "OMSTILLINGSSTØNAD", "DAGPENGER", "UFØRETRYGD"],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class ImplisittParameterizedTest

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = ["AAP", "OVERGANGSSTØNAD"],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class IkkeImplisittParameterizedTest
