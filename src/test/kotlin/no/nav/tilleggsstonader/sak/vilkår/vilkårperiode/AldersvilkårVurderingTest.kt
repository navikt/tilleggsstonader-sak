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
    fun `OVERGANGSSTØNAD skal gi svar JA_IMPLISITT`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.OVERGANGSSTØNAD)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA_IMPLISITT, it)
        }
    }

    @Test
    fun `AAP skal skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.AAP)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `NEDSATT_ARBEIDSEVNE skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.NEDSATT_ARBEIDSEVNE)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `UFØRETRYGD skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.UFØRETRYGD)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `DAGPENGER skal gi svar NEI`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.DAGPENGER)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.NEI, it)
        }
    }

    @Test
    fun `SYKEPENGER_100_PROSENT skal gi svar NEI`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.SYKEPENGER_100_PROSENT)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.NEI, it)
        }
    }

    @Test
    fun `INGEN_MÅLGRUPPE skal gi svar NEI`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.INGEN_MÅLGRUPPE)
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.NEI, it)
        }
    }

    @Test
    fun `AAP hvor bruker fyller 18 år midt i vilkårsperiode skal kaste feil`() {
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
    fun `AAP hvor bruker fyller 18 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(18)
        val fødselsår = fødselsdato.year

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val grunnlagsdata =
            grunnlagsdataDomain(
                grunnlag =
                    lagGrunnlagsdata(
                        fødsel =
                            Fødsel(
                                fødselsdato = fødselsdato,
                                fødselsår = fødselsår,
                            ),
                    ),
            )
        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, grunnlagsdata) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `AAP hvor bruker fyller 18 år dagen før vilkårsperioden starter skal ikke kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(18)
        val fødselsår = fødselsdato.year

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val grunnlagsdata =
            grunnlagsdataDomain(
                grunnlag =
                    lagGrunnlagsdata(
                        fødsel =
                            Fødsel(
                                fødselsdato = fødselsdato,
                                fødselsår = fødselsår,
                            ),
                    ),
            )
        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }

    @Test
    fun `AAP hvor bruker fyller 18 år dagen etter vilkårsperioden starter skal gi NEI`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(18)
        val fødselsår = fødselsdato.year

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val grunnlagsdata =
            grunnlagsdataDomain(
                grunnlag =
                    lagGrunnlagsdata(
                        fødsel =
                            Fødsel(
                                fødselsdato = fødselsdato,
                                fødselsår = fødselsår,
                            ),
                    ),
            )
        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.NEI, it)
        }
    }

    @Test
    fun `AAP hvor bruker fyller 18 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(18)
        val fødselsår = fødselsdato.year

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val grunnlagsdata =
            grunnlagsdataDomain(
                grunnlag =
                    lagGrunnlagsdata(
                        fødsel =
                            Fødsel(
                                fødselsdato = fødselsdato,
                                fødselsår = fødselsår,
                            ),
                    ),
            )
        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, grunnlagsdata) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }
}
