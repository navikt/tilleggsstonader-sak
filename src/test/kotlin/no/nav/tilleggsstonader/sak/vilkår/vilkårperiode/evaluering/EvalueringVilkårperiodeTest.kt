package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.evaulerVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EvalueringVilkårperiodeTest {

    @Test
    fun `skal evaluere vilkårsperiode når type og delvilkår matcher`() {
        val resultatMålgruppe =
            evaulerVilkårperiode(
                MålgruppeType.OMSTILLINGSSTØNAD,
                DelvilkårMålgruppeDto(medlemskap = VurderingDto(SvarJaNei.JA), dekketAvAnnetRegelverk = null),
            )
        assertThat(resultatMålgruppe.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

        val resultatAktivitet = evaulerVilkårperiode(
            AktivitetType.TILTAK,
            DelvilkårAktivitetDto(VurderingDto(SvarJaNei.NEI)),
        )
        assertThat(resultatAktivitet.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

    @Test
    fun `skal kaste feil hvis delvilkår ikke matcher type`() {
        assertThatThrownBy {
            evaulerVilkårperiode(MålgruppeType.AAP, DelvilkårAktivitetDto(VurderingDto(null)))
        }.hasMessageContaining("Ugyldig kombinasjon")

        assertThatThrownBy {
            evaulerVilkårperiode(AktivitetType.TILTAK, DelvilkårMålgruppeDto(null, null))
        }.hasMessageContaining("Ugyldig kombinasjon")
    }
}
