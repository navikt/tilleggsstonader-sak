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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.oppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.validerVilkårOgBeregnResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.ikkeOppfylteDelvilkårLøpendeUtgifterEnBoligDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.ikkeOppfylteDelvilkårLøpendeUtgifterToBoligerDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.ikkeOppfylteDelvilkårUtgifterOvernattingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterEnBolig
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterEnBoligDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterToBoliger
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterToBoligerDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårUtgifterOvernatting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårUtgifterOvernattingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.ikkeOppfylteDelvilkårPassBarnDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
                    erNullvedtak = false,
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
                        delvilkårsett = oppfylteDelvilkårUtgifterOvernattingDto(),
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        utgift = 1,
                        erNullvedtak = false,
                    )
                val utgifterOvernattingVilkår =
                    vilkår(
                        behandlingId = behandlingId,
                        type = VilkårType.UTGIFTER_OVERNATTING,
                        delvilkår = oppfylteDelvilkårUtgifterOvernatting(),
                    )

                @Test
                fun `skal validere at man har med fom og tom for vilkår for utgfiter overnatting`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(utgifterOvernattingVilkår, opprettUtgifterOvernattingVilkårDto.copy(fom = null))
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")

                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(utgifterOvernattingVilkår, opprettUtgifterOvernattingVilkårDto.copy(tom = null))
                    }.hasMessageContaining("Mangler fra og med/til og med på vilkår")
                }

                @Test
                fun `skal kaste feil hvis innvilget utgfiter overnatting ikke inneholder beløp`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(
                            utgifterOvernattingVilkår,
                            opprettUtgifterOvernattingVilkårDto.copy(utgift = null),
                        )
                    }.hasMessageContaining("Mangler utgift på vilkår")
                }

                @Test
                fun `skal ikke kaste feil hvis ikke oppfylt utgfiter overnatting ikke inneholder beløp`() {
                    val dto =
                        opprettUtgifterOvernattingVilkårDto.copy(
                            utgift = null,
                            delvilkårsett = ikkeOppfylteDelvilkårUtgifterOvernattingDto(),
                        )
                    validerVilkårOgBeregnResultat(utgifterOvernattingVilkår, dto)
                }

                @Test
                fun `oppfylt utgfiter overnatting kan mangle utgift når det er et nullvedtak`() {
                    validerVilkårOgBeregnResultat(
                        utgifterOvernattingVilkår,
                        opprettUtgifterOvernattingVilkårDto.copy(utgift = null, erNullvedtak = true),
                    )
                }

                @Test
                fun `skal ikke kaste feil hvis oppfylt utgfiter overnatting nullvedttak inneholder beløp`() {
                    assertThatThrownBy {
                        validerVilkårOgBeregnResultat(
                            utgifterOvernattingVilkår,
                            opprettUtgifterOvernattingVilkårDto.copy(utgift = 1, erNullvedtak = true),
                        )
                    }.hasMessageContaining("Kan ikke ha utgift på nullvedtak")
                }
            }

            @Nested
            inner class LøpendeUtgifterEnBolig {
                val opprettVilkårDto =
                    OpprettVilkårDto(
                        vilkårType = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                        behandlingId = behandlingId,
                        delvilkårsett = oppfylteDelvilkårLøpendeUtgifterEnBoligDto(),
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        utgift = 1,
                        erNullvedtak = false,
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
                            delvilkårsett = ikkeOppfylteDelvilkårLøpendeUtgifterEnBoligDto(),
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
                        delvilkårsett = oppfylteDelvilkårLøpendeUtgifterToBoligerDto(),
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        utgift = 1,
                        erNullvedtak = false,
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
                fun `utgift kan mangle når utgfiter overnatting ikke er oppfylt`() {
                    val dto =
                        opprettVilkårDto.copy(
                            utgift = null,
                            delvilkårsett = ikkeOppfylteDelvilkårLøpendeUtgifterToBoligerDto(),
                        )
                    validerVilkårOgBeregnResultat(vilkår, dto)
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

        private fun innsendtOppdatering(originaltVilkår: Vilkår) =
            SvarPåVilkårDto(
                id = originaltVilkår.id,
                behandlingId = behandling.id,
                delvilkårsett = oppfylteDelvilkårPassBarnDto(),
                fom = originaltVilkår.fom,
                tom = originaltVilkår.tom,
                utgift = 100,
                erNullvedtak = false,
            )
    }
}
