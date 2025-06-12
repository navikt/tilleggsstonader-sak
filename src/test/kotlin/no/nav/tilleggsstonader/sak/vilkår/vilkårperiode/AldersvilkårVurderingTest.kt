package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagFødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering.vurderAldersvilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AldersvilkårVurderingTest {
    @Test
    fun `Gyldige perioder med målgruppe OMSTILLINGSSTØNAD skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe()
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Gyldige perioder med målgruppe AAP skal skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.AAP)
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Gyldige perioder med målgruppe NEDSATT_ARBEIDSEVNE skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.NEDSATT_ARBEIDSEVNE)
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Gyldige perioder med målgruppe UFØRETRYGD skal gi svar JA`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.UFØRETRYGD)
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Periode med målgruppe OVERGANGSSTØNAD skal kaste feil`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.OVERGANGSSTØNAD)
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Aldersvilkår vurderes ikke for målgruppe: OVERGANGSSTØNAD")
    }

    @Test
    fun `Periode med målgruppe DAGPENGER skal kaste feil`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.DAGPENGER)
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Aldersvilkår vurderes ikke for målgruppe: DAGPENGER")
    }

    @Test
    fun `Periode med målgruppe SYKEPENGER_100_PROSENT skal kaste feil`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.SYKEPENGER_100_PROSENT)
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Aldersvilkår vurderes ikke for målgruppe: SYKEPENGER_100_PROSENT")
    }

    @Test
    fun `Periode med målgruppe INGEN_MÅLGRUPPE skal kaste feil`() {
        val målgruppe = dummyVilkårperiodeMålgruppe().copy(type = MålgruppeType.INGEN_MÅLGRUPPE)
        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag()

        val feil = assertThrows<Feil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Aldersvilkår vurderes ikke for målgruppe: INGEN_MÅLGRUPPE")
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 18 år dagen før vilkårsperioden starter skal gi svar JA`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 18 år dagen etter vilkårsperioden slutter skal gi svar NEI`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 18 år midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(osloDateNow().minusYears(18))

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 18 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 18 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 18 år dagen før vilkårsperioden starter skal gi svar JA`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 18 år dagen etter vilkårsperioden slutter skal gi svar NEI`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 18 år midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(osloDateNow().minusYears(18))

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 18 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)
        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 18 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)
        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 18 år dagen før vilkårsperioden starter skal gi svar JA`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 18 år dagen etter vilkårsperioden slutter skal gi svar NEI`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 18 år midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(osloDateNow().minusYears(18))

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 18 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 18 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(18)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 18 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 67 år dagen før vilkårsperioden starter skal gi svar NEI`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 67 år dagen etter vilkårsperioden slutter skal gi svar JA`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 67 år midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = osloDateNow().minusYears(67))

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 67 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe AAP hvor bruker fyller 67 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.AAP,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 67 år dagen før vilkårsperioden starter skal gi svar NEI`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 67 år dagen etter vilkårsperioden slutter skal gi svar JA`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 67 år midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = osloDateNow().minusYears(67))

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 67 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe NEDSATT_ARBEIDSEVNE hvor bruker fyller 67 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)
        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 67 år dagen før vilkårsperioden starter skal gi svar NEI`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 67 år dagen etter vilkårsperioden slutter skal gi svar JA`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 67 år midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = osloDateNow().minusYears(67))

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 67 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe UFØRETRYGD hvor bruker fyller 67 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.UFØRETRYGD,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe OMSTILLINGSSTØNAD hvor bruker fyller 67 år dagen før vilkårsperioden starter skal gi svar NEI`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.OMSTILLINGSSTØNAD,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `Målgruppe OMSTILLINGSSTØNAD hvor bruker fyller 67 år dagen etter vilkårsperioden slutter skal gi svar JA`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.plusDays(1).minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.OMSTILLINGSSTØNAD,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        assertThat(vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag)).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `Målgruppe OMSTILLINGSSTØNAD hvor bruker fyller 67 år midt i vilkårsperiode skal kaste feil`() {
        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.OMSTILLINGSSTØNAD,
                fom = osloDateNow().minusDays(10),
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = osloDateNow().minusYears(67))

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe OMSTILLINGSSTØNAD hvor bruker fyller 67 år første dag i vilkårsperiode skal kaste feil`() {
        val fom = osloDateNow().minusDays(10)
        val fødselsdato = fom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.OMSTILLINGSSTØNAD,
                fom = fom,
                tom = osloDateNow().plusDays(10),
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }

    @Test
    fun `Målgruppe OMSTILLINGSSTØNAD hvor bruker fyller 67 år siste dag i vilkårsperiode skal kaste feil`() {
        val tom = osloDateNow().plusDays(10)
        val fødselsdato = tom.minusYears(67)

        val målgruppe =
            dummyVilkårperiodeMålgruppe().copy(
                type = MålgruppeType.OMSTILLINGSSTØNAD,
                fom = osloDateNow().minusDays(10),
                tom = tom,
            )

        val fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = fødselsdato)

        val feil = assertThrows<ApiFeil> { vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag) }
        assertThat(feil.message).isEqualTo("Brukeren fyller 67 år i løpet av vilkårsperioden")
    }
}
