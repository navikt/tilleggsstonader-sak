package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.evaulerVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EvalueringVilkårperiodeTest {

    @Test
    fun `skal evaluere gyldig kombinasjon av målgruppe`() {
        val resultatMålgruppe =
            evaulerVilkårperiode(MålgruppeType.OMSTILLINGSSTØNAD, DelvilkårMålgruppeDto(SvarJaNei.JA))
        assertThat(resultatMålgruppe.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

    @Test
    fun `skal evaluere gyldig kombinasjon av aktivitet`() {
        val resultatAktivitet = evaulerVilkårperiode(
            AktivitetType.TILTAK,
            DelvilkårAktivitetDto(SvarJaNei.NEI, SvarJaNei.NEI),
        )
        assertThat(resultatAktivitet.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

    @Test
    fun `skal kaste feil hvis man kaller med feil type`() {
        assertThatThrownBy {
            evaulerVilkårperiode(MålgruppeType.AAP, DelvilkårAktivitetDto(null, null))
        }.hasMessageContaining("Ugyldig kombinasjon")

        assertThatThrownBy {
            evaulerVilkårperiode(AktivitetType.TILTAK, DelvilkårMålgruppeDto(null))
        }.hasMessageContaining("Ugyldig kombinasjon")
    }
}
