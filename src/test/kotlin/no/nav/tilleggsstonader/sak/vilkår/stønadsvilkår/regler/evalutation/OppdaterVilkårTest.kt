package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.oppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.validerVilkårOgBeregnResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.delvilkårFremtidigeUtgifter
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.ikkeOppfylteDelvilkårLøpendeUtgifterEnBolig
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.ikkeOppfylteDelvilkårLøpendeUtgifterToBoliger
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.ikkeOppfylteDelvilkårUtgifterOvernatting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterEnBolig
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterToBoliger
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterToBoligerHøyereUtgifterHelsemessigÅrsaker
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårUtgifterOvernatting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.ikkeOppfylteDelvilkårPassBarnDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

internal class OppdaterVilkårTest {
    val behandlingId = BehandlingId.random()
    val vilkår =
        vilkår(
            behandlingId = behandlingId,
            type = VilkårType.PASS_BARN,
            delvilkår = oppfylteDelvilkårPassBarn(),
        )

    @Nested
    inner class ValideringAvBeløp {
        @Nested
        inner class TilsynBarn {
            val opprettVilkårDto =
                OpprettVilkårDto(
                    vilkårType = VilkårType.PASS_BARN,
                    barnId = BarnId.random(),
                    behandlingId = behandlingId,
                    delvilkårsett = oppfylteDelvilkårPassBarnDto(),
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(1),
                    utgift = 1,
                    erFremtidigUtgift = false,
                )

            @Test
            fun `skal validere at man har med fom og tom for vilkår for pass av barn`() {
                assertThatThrownBy {
                    validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(fom = null))
                }.hasMessageContaining("Mangler fra og med/til og med på vilkår")

                assertThatThrownBy {
                    validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(tom = null))
                }.hasMessageContaining("Mangler fra og med/til og med på vilkår")
            }

            @Test
            fun `skal kaste feil hvis innvilget pass av barn ikke inneholder beløp`() {
                assertThatThrownBy {
                    validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(utgift = null))
                }.hasMessageContaining("Mangler utgift på vilkår")
            }

            @Test
            fun `skal ikke kaste feil hvis ikke oppfylt pass av barn ikke inneholder beløp`() {
                val dto = opprettVilkårDto.copy(utgift = null, delvilkårsett = ikkeOppfylteDelvilkårPassBarnDto())
                validerVilkårOgBeregnResultat(vilkår, dto)
            }
        }

        @Nested
        inner class Boutgifter {
            @Nested
            inner class UtgifterOvernatting {
                val opprettUtgifterOvernattingVilkårDto =
                    OpprettVilkårDto(
                        vilkårType = VilkårType.UTGIFTER_OVERNATTING,
                        behandlingId = behandlingId,
                        delvilkårsett = oppfylteDelvilkårUtgifterOvernatting().map { it.tilDto() },
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        utgift = 1,
                        erFremtidigUtgift = false,
                    )
                val utgifterOvernattingVilkår =
                    vilkår(
                        behandlingId = behandlingId,
                        type = VilkårType.UTGIFTER_OVERNATTING,
                        delvilkår = oppfylteDelvilkårUtgifterOvernatting(),
                    )
                val opprettUtgifterFremtidigUtgiftDto =
                    OpprettVilkårDto(
                        vilkårType = VilkårType.UTGIFTER_OVERNATTING,
                        behandlingId = behandlingId,
                        delvilkårsett = delvilkårFremtidigeUtgifter().map { it.tilDto() },
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        utgift = 1,
                        erFremtidigUtgift = false,
                    )
                val utgifterFremtidigUtgiftVilkår =
                    vilkår(
                        behandlingId = behandlingId,
                        type = VilkårType.UTGIFTER_OVERNATTING,
                        delvilkår = delvilkårFremtidigeUtgifter(),
                    )

                @Test
                fun `skal validere at man har med fom og tom for vilkår for utgifter overnatting`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(
                            utgifterOvernattingVilkår,
                            opprettUtgifterOvernattingVilkårDto.copy(fom = null),
                        )
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")

                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(
                            utgifterOvernattingVilkår,
                            opprettUtgifterOvernattingVilkårDto.copy(tom = null),
                        )
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")
                }

                @Test
                fun `skal kaste feil hvis innvilget utgifter overnatting ikke inneholder beløp`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(
                            utgifterOvernattingVilkår,
                            opprettUtgifterOvernattingVilkårDto.copy(utgift = null),
                        )
                    }.hasMessageContaining("Mangler utgift på vilkår")
                }

                @Test
                fun `skal ikke kaste feil hvis ikke oppfylt utgifter overnatting ikke inneholder beløp`() {
                    val dto =
                        opprettUtgifterOvernattingVilkårDto.copy(
                            utgift = null,
                            delvilkårsett = ikkeOppfylteDelvilkårUtgifterOvernatting().map { it.tilDto() },
                        )
                    validerVilkårOgBeregnResultat(utgifterOvernattingVilkår, dto)
                }

                @Test
                fun `oppfylt utgifter overnatting kan mangle utgift når det er fremtidig utgift`() {
                    validerVilkårOgBeregnResultat(
                        utgifterFremtidigUtgiftVilkår,
                        opprettUtgifterFremtidigUtgiftDto.copy(utgift = null, erFremtidigUtgift = true),
                    )
                }

                @Test
                internal fun `Skal kunne oppdatere vilkår som fremtidig utgift`() {
                    val dto =
                        opprettUtgifterOvernattingVilkårDto.copy(
                            utgift = 1000,
                            delvilkårsett = delvilkårFremtidigeUtgifter().map { it.tilDto() },
                            erFremtidigUtgift = true,
                        )
                    assertDoesNotThrow { validerVilkårOgBeregnResultat(utgifterOvernattingVilkår, dto) }
                }

                @Test
                internal fun `Skal ikke ha vilkårsvurdert når fremtidig utgift`() {
                    val dto =
                        opprettUtgifterOvernattingVilkårDto.copy(
                            utgift = 1000,
                            delvilkårsett = oppfylteDelvilkårUtgifterOvernatting().map { it.tilDto() },
                            erFremtidigUtgift = true,
                        )
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(
                            utgifterOvernattingVilkår,
                            dto,
                        )
                    }.hasMessageContaining("Kan ikke ha svar på vilkår når fremtidig utgift er valgt")
                }
            }

            @Nested
            inner class LøpendeUtgifterEnBolig {
                val opprettVilkårDto =
                    OpprettVilkårDto(
                        vilkårType = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                        behandlingId = behandlingId,
                        delvilkårsett = oppfylteDelvilkårLøpendeUtgifterEnBolig().map { it.tilDto() },
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        utgift = 1,
                        erFremtidigUtgift = false,
                    )
                val vilkår =
                    vilkår(
                        behandlingId = behandlingId,
                        type = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                        delvilkår = oppfylteDelvilkårLøpendeUtgifterEnBolig(),
                    )

                @Test
                fun `skal validere at man har med fom og tom for vilkår for løpende utgifter en bolig`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(fom = null))
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")

                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(tom = null))
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")
                }

                @Test
                fun `skal kaste feil hvis innvilget løpende utgifter en bolig ikke inneholder beløp`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(utgift = null))
                    }.hasMessageContaining("Mangler utgift på vilkår")
                }

                @Test
                fun `skal ikke kaste feil hvis ikke oppfylt løpende utgifter en bolig ikke inneholder beløp`() {
                    val dto =
                        opprettVilkårDto.copy(
                            utgift = null,
                            delvilkårsett = ikkeOppfylteDelvilkårLøpendeUtgifterEnBolig().map { it.tilDto() },
                        )
                    validerVilkårOgBeregnResultat(vilkår, dto)
                }
            }

            @Nested
            inner class LøpendeUtgifterToBoliger {
                val opprettVilkårDto =
                    OpprettVilkårDto(
                        vilkårType = VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER,
                        behandlingId = behandlingId,
                        delvilkårsett = oppfylteDelvilkårLøpendeUtgifterToBoliger().map { it.tilDto() },
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        utgift = 1,
                        erFremtidigUtgift = false,
                    )
                val vilkår =
                    vilkår(
                        behandlingId = behandlingId,
                        type = VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER,
                        delvilkår = oppfylteDelvilkårLøpendeUtgifterToBoliger(),
                    )

                @Test
                fun `skal validere at man har med fom og tom for vilkår for løpende utgifter to boliger`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(fom = null))
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")

                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(tom = null))
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")
                }

                @Test
                fun `skal kaste feil hvis innvilget løpende utgifter to boliger ikke inneholder beløp`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(utgift = null))
                    }.hasMessageContaining("Mangler utgift på vilkår")
                }

                @Test
                fun `utgift kan mangle når utgifter overnatting ikke er oppfylt`() {
                    val dto =
                        opprettVilkårDto.copy(
                            utgift = null,
                            delvilkårsett = ikkeOppfylteDelvilkårLøpendeUtgifterToBoliger().map { it.tilDto() },
                        )
                    validerVilkårOgBeregnResultat(vilkår, dto)
                }

                @Test
                internal fun `Skal ikke kunne oppdatere vilkår som har Høyere utgifter grunnet helsemessig årsaker`() {
                    val dto =
                        opprettVilkårDto.copy(
                            utgift = 500,
                            delvilkårsett =
                                oppfylteDelvilkårLøpendeUtgifterToBoligerHøyereUtgifterHelsemessigÅrsaker().map {
                                    it.tilDto()
                                },
                        )

                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(vilkår, dto)
                    }.hasMessageContaining(
                        "Vi støtter ikke beregning med \"Høyere utgifter grunnet helsemessig årsaker\". Ta kontakt med Tilleggsstønader teamet.",
                    )
                }
            }
        }
    }

    @Nested
    inner class StatusPåVilkår {
        val behandling = behandling()
        val regelResultat =
            RegelResultat(
                vilkårType = VilkårType.PASS_BARN,
                vilkår = Vilkårsresultat.OPPFYLT,
                delvilkår = emptyMap(),
            )

        @Test
        fun `Nye vilkår skal ha status ny etter oppdatering`() {
            val vilkår = vilkår(behandling.id, type = VilkårType.PASS_BARN, status = VilkårStatus.NY)
            val innsendtOppdatering = innsendtOppdatering(vilkår)

            val oppdaterVilkår =
                oppdaterVilkår(vilkår = vilkår, oppdatering = innsendtOppdatering, vilkårsresultat = regelResultat)

            assertThat(oppdaterVilkår.status).isEqualTo(VilkårStatus.NY)
        }

        @Test
        fun `Vilkår med status endret skal ikke endre status ved oppdatering`() {
            val vilkår = vilkår(behandling.id, type = VilkårType.PASS_BARN, status = VilkårStatus.ENDRET)
            val innsendtOppdatering = innsendtOppdatering(vilkår)

            val oppdaterVilkår =
                oppdaterVilkår(vilkår = vilkår, oppdatering = innsendtOppdatering, vilkårsresultat = regelResultat)

            assertThat(oppdaterVilkår.status).isEqualTo(VilkårStatus.ENDRET)
        }

        @Test
        fun `Vilkår med status uendret skal få status endret etter oppdatering`() {
            val vilkår = vilkår(behandling.id, type = VilkårType.PASS_BARN, status = VilkårStatus.UENDRET)
            val innsendtOppdatering = innsendtOppdatering(vilkår)

            val oppdaterVilkår =
                oppdaterVilkår(vilkår = vilkår, oppdatering = innsendtOppdatering, vilkårsresultat = regelResultat)

            assertThat(oppdaterVilkår.status).isEqualTo(VilkårStatus.ENDRET)
        }

        @Test
        fun `Vilkår som er fremtidig utgift skal få status ny når de gjøres om til vanlig vilkår`() {
            val vilkår = vilkår(behandling.id, type = VilkårType.PASS_BARN, status = VilkårStatus.UENDRET, erFremtidigUtgift = true)
            val innsendtOppdatering = innsendtOppdatering(vilkår.copy(erFremtidigUtgift = false))

            val oppdaterVilkår =
                oppdaterVilkår(vilkår = vilkår, oppdatering = innsendtOppdatering, vilkårsresultat = regelResultat)

            assertThat(oppdaterVilkår.status).isEqualTo(VilkårStatus.NY)
        }

        private fun innsendtOppdatering(originaltVilkår: Vilkår) =
            SvarPåVilkårDto(
                id = originaltVilkår.id,
                behandlingId = behandling.id,
                delvilkårsett = oppfylteDelvilkårPassBarnDto(),
                fom = originaltVilkår.fom,
                tom = originaltVilkår.tom,
                utgift = 100,
                erFremtidigUtgift = false,
            )
    }
}
