package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VilkårperiodeTest {

    @Nested
    inner class ValideringTypeDetaljer {

        @Test
        fun `målgruppe gyldig kombinasjon`() {
            målgruppe()
        }

        @Test
        fun `målgruppe ugyldige detaljer kaster feil`() {
            assertThatThrownBy {
                målgruppe().copy(delvilkår = delvilkårAktivitet())
            }.hasMessageContaining("Ugyldig kombinasjon")
        }

        @Test
        fun `aktivitet gyldig kombinasjon`() {
            aktivitet()
        }

        @Test
        fun `aktivitet ugyldige detaljer kaster feil`() {
            assertThatThrownBy {
                aktivitet().copy(delvilkår = delvilkårMålgruppe())
            }.hasMessageContaining("Ugyldig kombinasjon")
        }
    }

    @Nested
    inner class ValideringSlettet {

        @Test
        fun `kan ikke slette periode som er opprettet av systemet`() {
            assertThatThrownBy {
                målgruppe(kilde = KildeVilkårsperiode.SYSTEM).copy(resultat = ResultatVilkårperiode.SLETTET)
            }.hasMessageContaining("Kan ikke slette når kilde=")
        }

        @Test
        fun `kan ha kommentar når resultat er slettet`() {
            målgruppe(kilde = KildeVilkårsperiode.MANUELL)
                .copy(resultat = ResultatVilkårperiode.SLETTET, slettetKommentar = "Abc")
        }

        @Test
        fun `feiler hvis man mangler kommentar når resultat er slettet`() {
            assertThatThrownBy {
                målgruppe(kilde = KildeVilkårsperiode.MANUELL)
                    .copy(resultat = ResultatVilkårperiode.SLETTET)
            }.hasMessageContaining("Mangler kommentar for resultat=")
        }

        @Test
        fun `feiler hvis man har kommentar når resultat er slettet`() {
            assertThatThrownBy {
                målgruppe(kilde = KildeVilkårsperiode.MANUELL)
                    .copy(slettetKommentar = "Abc")
            }.hasMessageContaining("Kan ikke ha slettetkommentar")
        }
    }
}
