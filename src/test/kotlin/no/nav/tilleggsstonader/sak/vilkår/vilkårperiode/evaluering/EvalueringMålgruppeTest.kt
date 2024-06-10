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
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EvalueringMålgruppeTest {

    val jaVurdering = VurderingDto(SvarJaNei.JA)
    val implisittVurdering = VurderingDto(SvarJaNei.JA_IMPLISITT)
    val neiVurdering = VurderingDto(SvarJaNei.NEI)
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

        @Test
        fun `ingen målgruppe skal mappes til ikke oppfylt`() {
            val resultat = utledResultat(
                MålgruppeType.INGEN_MÅLGRUPPE,
                delvilkårMålgruppeDto(
                    medlemskap = VurderingDto(svar = null),
                    dekketAvAnnetRegelverk = VurderingDto(svar = null),
                ),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(resultat.medlemskap.svar).isNull()
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_AKTUELT)
            assertThat(resultat.dekketAvAnnetRegelverk.svar).isNull()
            assertThat(resultat.dekketAvAnnetRegelverk.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_AKTUELT)
        }
    }

    @Nested
    inner class UtgifterDekketAvAnnetRegelverk {

        @Nested
        inner class NedsattArbeidsevne {
            @NedsattArbeidsevneParameterizedTest
            fun `svar nei skal mappes til oppfylt for nedsatt arbeidsevne`(type: MålgruppeType) {
                val resultat = utledResultat(
                    type,
                    delvilkårMålgruppeDto(
                        medlemskap = oppfyltMedlemskap(type),
                        dekketAvAnnetRegelverk = VurderingDto(svar = SvarJaNei.NEI),
                    ),
                )

                assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(resultat.dekketAvAnnetRegelverk.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            }

            @NedsattArbeidsevneParameterizedTest
            fun `svar ja skal mappes til ikke oppfylt for nedsatt arbeidsevne`(type: MålgruppeType) {
                val resultat = utledResultat(
                    type,
                    delvilkårMålgruppeDto(
                        medlemskap = oppfyltMedlemskap(type),
                        dekketAvAnnetRegelverk = VurderingDto(svar = SvarJaNei.JA),
                    ),
                )

                assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                assertThat(resultat.dekketAvAnnetRegelverk.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            }

            @NedsattArbeidsevneParameterizedTest
            fun `manglende svar skal mappes til ikke vurdert for nedsatt arbeidsevne`(type: MålgruppeType) {
                val resultat = utledResultat(
                    type,
                    delvilkårMålgruppeDto(
                        medlemskap = oppfyltMedlemskap(type),
                        dekketAvAnnetRegelverk = VurderingDto(svar = null),
                    ),
                )

                assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
                assertThat(resultat.dekketAvAnnetRegelverk.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
            }

            @NedsattArbeidsevneParameterizedTest
            fun `implisitt svar skal kaste feil`(type: MålgruppeType) {
                assertThatThrownBy {
                    utledResultat(
                        type,
                        delvilkårMålgruppeDto(
                            medlemskap = oppfyltMedlemskap(type),
                            dekketAvAnnetRegelverk = VurderingDto(svar = SvarJaNei.JA_IMPLISITT),
                        ),
                    )
                }.hasMessageContaining("Ugyldig svar=JA_IMPLISITT for dekket av annet regelverk for $type")
            }
        }

        @Nested
        inner class MålgrupperUtenDekketAvAnnetRegelverk {
            @IkkeNedsattArbeidsevneParameterizedTest
            fun `manglende svar skal mappes til oppfylt`(type: MålgruppeType) {
                val resultat = utledResultat(
                    type,
                    delvilkårMålgruppeDto(
                        medlemskap = oppfyltMedlemskap(type),
                        dekketAvAnnetRegelverk = VurderingDto(svar = null),
                    ),
                )

                assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(resultat.dekketAvAnnetRegelverk.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_AKTUELT)
            }

            @IkkeNedsattArbeidsevneParameterizedTest
            fun `skal kaste feil om svar er satt`(type: MålgruppeType) {
                assertThatThrownBy {
                    utledResultat(
                        type,
                        delvilkårMålgruppeDto(
                            medlemskap = oppfyltMedlemskap(type),
                            dekketAvAnnetRegelverk = VurderingDto(svar = SvarJaNei.JA),
                        ),
                    )
                }.hasMessageContaining("Ugyldig svar=JA for dekket av annet regelverk for $type")

                assertThatThrownBy {
                    utledResultat(
                        type,
                        delvilkårMålgruppeDto(
                            medlemskap = oppfyltMedlemskap(type),
                            dekketAvAnnetRegelverk = VurderingDto(svar = SvarJaNei.NEI),
                        ),
                    )
                }.hasMessageContaining("Ugyldig svar=NEI for dekket av annet regelverk for $type")

                assertThatThrownBy {
                    utledResultat(
                        type,
                        delvilkårMålgruppeDto(
                            medlemskap = oppfyltMedlemskap(type),
                            dekketAvAnnetRegelverk = VurderingDto(svar = SvarJaNei.JA_IMPLISITT),
                        ),
                    )
                }.hasMessageContaining("Ugyldig svar=JA_IMPLISITT for dekket av annet regelverk for $type")
            }
        }
    }

    @Nested
    inner class Sykepenger {

        @Test
        fun `100 prosent sykepenger skal mappes til ikke oppfylt`() {
            val resultat = utledResultat(
                MålgruppeType.SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING,
                delvilkårMålgruppeDto(
                    medlemskap = VurderingDto(svar = null),
                    dekketAvAnnetRegelverk = VurderingDto(svar = null),
                ),
            )

            assertThat(resultat.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(resultat.medlemskap.svar).isNull()
            assertThat(resultat.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_AKTUELT)
            assertThat(resultat.dekketAvAnnetRegelverk.svar).isNull()
            assertThat(resultat.dekketAvAnnetRegelverk.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_AKTUELT)
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

    private fun oppfyltMedlemskap(type: MålgruppeType): VurderingDto {
        if (type == MålgruppeType.AAP || type == MålgruppeType.OVERGANGSSTØNAD) {
            return VurderingDto(svar = SvarJaNei.JA_IMPLISITT)
        }
        return VurderingDto(svar = SvarJaNei.JA)
    }
}

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = [
        "NEDSATT_ARBEIDSEVNE",
        "OMSTILLINGSSTØNAD",
        "DAGPENGER",
        "UFØRETRYGD",
        "SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING",
        "INGEN_MÅLGRUPPE",
    ],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class ImplisittParameterizedTest

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = [
        "AAP",
        "OVERGANGSSTØNAD",
        "SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING",
        "INGEN_MÅLGRUPPE",
    ],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class IkkeImplisittParameterizedTest

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = [
        "OVERGANGSSTØNAD",
        "OMSTILLINGSSTØNAD",
        "DAGPENGER",
        "SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING",
        "INGEN_MÅLGRUPPE",
    ],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class NedsattArbeidsevneParameterizedTest

@ParameterizedTest
@EnumSource(
    value = MålgruppeType::class,
    names = [
        "AAP",
        "NEDSATT_ARBEIDSEVNE",
        "UFØRETRYGD",
        "SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING",
        "INGEN_MÅLGRUPPE",
    ],
    mode = EnumSource.Mode.EXCLUDE,
)
private annotation class IkkeNedsattArbeidsevneParameterizedTest
