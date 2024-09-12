package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.VilkårGrunnlagUtil.grunnlagBarn
import no.nav.tilleggsstonader.sak.util.VilkårGrunnlagUtil.mockVilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VilkårsoppsummeringGammelDtoServiceTest {
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val stønadsperiodeService = mockk<StønadsperiodeService>()
    val vilkårService = mockk<VilkårService>()
    val behandlingFaktaService = mockk<BehandlingFaktaService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()

    val vilkårsoppsummeringService =
        VilkårsoppsummeringService(
            vilkårperiodeService,
            stønadsperiodeService,
            vilkårService,
            behandlingFaktaService,
            grunnlagsdataService,
        )

    val fom = LocalDate.now()
    val tom = LocalDate.now().plusMonths(1)
    val barnId = UUID.randomUUID()

    val aktivitet = aktivitet(
        resultat = ResultatVilkårperiode.OPPFYLT,
        fom = fom,
        tom = tom,
        type = AktivitetType.TILTAK,
    )

    val målgruppe =
        målgruppe(resultat = ResultatVilkårperiode.OPPFYLT, fom = fom, tom = tom, type = MålgruppeType.AAP)

    val stønadsperiode = StønadsperiodeDto(
        fom = fom,
        tom = tom,
        aktivitet = AktivitetType.TILTAK,
        målgruppe = MålgruppeType.AAP,
    )

    val vilkårPassBarn = vilkår(
        behandlingId = UUID.randomUUID(),
        resultat = Vilkårsresultat.OPPFYLT,
        type = VilkårType.PASS_BARN,
        barnId = barnId,
    )

    val behandlingFakta = mockVilkårGrunnlagDto(
        barn = listOf(
            grunnlagBarn(barnId = barnId),
        ),
    )

    @Nested
    inner class OppfylteVilkårperioderOgStønadsperiode {
        @BeforeEach
        fun setUp() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(
                målgrupper = listOf(målgruppe),
                aktiviteter = listOf(aktivitet),
            )
            every { stønadsperiodeService.hentStønadsperioder(any()) } returns listOf(stønadsperiode)
            every { vilkårService.hentPassBarnVilkår(any()) } returns listOf(vilkårPassBarn)
            every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns grunnlagsdataDomain()
        }

        @Test
        fun `alle perioder og vilkår er oppfylt skal bare gi positive resultater`() {
            every { behandlingFaktaService.hentFakta(any()) } returns behandlingFakta

            val res = vilkårsoppsummeringService.hentVilkårsoppsummering(UUID.randomUUID())

            assertThat(res.aktivitet).isTrue()
            assertThat(res.målgruppe).isTrue()
            assertThat(res.stønadsperiode).isTrue()
            assertThat(res.passBarn).hasSize(1)
            res.passBarn.forEach {
                assertThat(it.oppfyllerAlleVilkår).isTrue()
                assertThat(it.barnId).isEqualTo(barnId)
            }
        }

        @Test
        fun `skal kaste feil dersom vilkår id ikke matcher`() {
            every { behandlingFaktaService.hentFakta(any()) } returns mockVilkårGrunnlagDto(
                barn = listOf(
                    grunnlagBarn(barnId = UUID.randomUUID()),
                ),
            )

            assertThatThrownBy {
                vilkårsoppsummeringService.hentVilkårsoppsummering(UUID.randomUUID())
            }.hasMessageContaining("Fant ikke barn med id")
        }
    }

    @Test
    fun `ikke oppfylte perioder og vilkår skal gi negative resultater`() {
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(
            målgrupper = listOf(målgruppe.copy(resultat = ResultatVilkårperiode.IKKE_OPPFYLT)),
            aktiviteter = listOf(aktivitet.copy(resultat = ResultatVilkårperiode.IKKE_OPPFYLT)),
        )
        every { stønadsperiodeService.hentStønadsperioder(any()) } returns emptyList()
        every { vilkårService.hentPassBarnVilkår(any()) } returns listOf(vilkårPassBarn.copy(resultat = Vilkårsresultat.IKKE_OPPFYLT))

        every { behandlingFaktaService.hentFakta(any()) } returns behandlingFakta

        val res = vilkårsoppsummeringService.hentVilkårsoppsummering(UUID.randomUUID())

        assertThat(res.aktivitet).isFalse()
        assertThat(res.målgruppe).isFalse()
        assertThat(res.stønadsperiode).isFalse()
        assertThat(res.passBarn).hasSize(1)
        res.passBarn.forEach {
            assertThat(it.oppfyllerAlleVilkår).isFalse()
            assertThat(it.barnId).isEqualTo(barnId)
        }
    }
}
