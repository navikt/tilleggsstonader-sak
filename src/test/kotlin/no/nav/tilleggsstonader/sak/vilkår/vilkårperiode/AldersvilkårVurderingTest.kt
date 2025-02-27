package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Fødsel
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering.vurderAldersvilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AldersvilkårVurderingTest {
    @Test
    fun `OMSTILLINGSSTØNAD skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe()
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `OVERGANGSSTØNAD gi svar JA_IMPLISITT`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.OVERGANGSSTØNAD)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA_IMPLISITT, it)
        }
    }

    @Test
    fun `AAP gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.AAP)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `NEDSATT_ARBEIDSEVNE gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.NEDSATT_ARBEIDSEVNE)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `UFØRETRYGD gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.UFØRETRYGD)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `DAGPENGER gi svar NEI`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.DAGPENGER)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.NEI, it)
        }
    }

    @Test
    fun `SYKEPENGER_100_PROSENT gi svar NEI`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.SYKEPENGER_100_PROSENT)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.NEI, it)
        }
    }

    @Test
    fun `INGEN_MÅLGRUPPE gi svar NEI`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.INGEN_MÅLGRUPPE)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.NEI, it)
        }
    }

    @Test
    fun `AAP med 18-års dag midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val grunnlagsdata =
            grunnlagsdataDomain(
                grunnlag =
                    lagGrunnlagsdata(
                        fødsel =
                            Fødsel(
                                fødselsdato = osloDateNow().minusYears(18),
                                osloDateNow().minusYears(18).year,
                            ),
                    ),
            )
        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, grunnlagsdata) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `AAP med 18-års dag første dag i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val grunnlagsdata =
            grunnlagsdataDomain(
                grunnlag =
                    lagGrunnlagsdata(
                        fødsel =
                            Fødsel(
                                fødselsdato = osloDateNow().minusYears(18).minusDays(10),
                                osloDateNow().minusYears(18).minusDays(10).year,
                            ),
                    ),
            )
        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, grunnlagsdata) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `AAP med 18-års dag dagen før vilkårsperioden starter skal ikke kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val grunnlagsdata =
            grunnlagsdataDomain(
                grunnlag =
                    lagGrunnlagsdata(
                        fødsel =
                            Fødsel(
                                fødselsdato = osloDateNow().minusYears(18).minusDays(11),
                                osloDateNow().minusYears(18).minusDays(11).year,
                            ),
                    ),
            )
        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }
}
