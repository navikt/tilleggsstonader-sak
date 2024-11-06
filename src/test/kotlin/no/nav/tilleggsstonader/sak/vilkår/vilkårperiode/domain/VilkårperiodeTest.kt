package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.medVilkårOgFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

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
                målgruppe().medVilkårOgFakta(delvilkår = delvilkårAktivitet())
            }.hasMessageContaining("Ugyldig kombinasjon")
        }

        @Test
        fun `aktivitet gyldig kombinasjon`() {
            aktivitet()
        }

        @Test
        fun `aktivitet ugyldige detaljer kaster feil`() {
            assertThatThrownBy {
                aktivitet().medVilkårOgFakta(delvilkår = delvilkårMålgruppe())
            }.hasMessageContaining("Ugyldig kombinasjon")
        }
    }

    @Nested
    inner class ValideringSlettet {

        @Test
        fun `kan ha kommentar når resultat er slettet`() {
            målgruppe()
                .copy(resultat = ResultatVilkårperiode.SLETTET, slettetKommentar = "Abc")
        }

        @Test
        fun `feiler hvis man mangler kommentar når resultat er slettet og perioden er gjenbrukt`() {
            assertThatThrownBy {
                målgruppe(forrigeVilkårperiodeId = UUID.randomUUID())
                    .copy(resultat = ResultatVilkårperiode.SLETTET)
            }.hasMessageContaining("Mangler kommentar for resultat=")
        }

        @Test
        fun `feiler hvis man har kommentar når resultat er slettet`() {
            assertThatThrownBy {
                målgruppe()
                    .copy(slettetKommentar = "Abc")
            }.hasMessageContaining("Kan ikke ha slettetkommentar")
        }
    }

    @Nested
    inner class ValideringSykepenger {
        @Test
        fun `100 prosent sykepenger må inneholde begrunnelse`() {
            assertThatThrownBy {
                målgruppe(type = MålgruppeType.SYKEPENGER_100_PROSENT)
            }.hasMessageContaining("Mangler begrunnelse for 100% sykepenger")
        }
    }

    @Nested
    inner class GjenbrukUtledForrigeVilkårPeriodeId {

        private val behandlingId = BehandlingId.random()

        @Test
        fun `man skal ikke kopiere vilkår som er slettet`() {
            assertThatThrownBy {
                målgruppe(status = Vilkårstatus.SLETTET).kopierTilBehandling(behandlingId).forrigeVilkårperiodeId
            }.hasMessageContaining("Skal ikke kopiere vilkårperiode som er slettet")
        }

        @Test
        fun `skal feile et vilkår med status uendret ikke inneholder forrigeVilkårPeriode, då det ellers ikke kan ha status UENDRET`() {
            assertThatThrownBy {
                målgruppe(status = Vilkårstatus.UENDRET).kopierTilBehandling(behandlingId).forrigeVilkårperiodeId
            }.hasMessageContaining("Forventer at vilkårperiode med status=UENDRET har forrigeVilkårperiodeId")
        }

        @Test
        fun `kopiering av et vilkår med status UENDRET skal peke til forrigeVilkårPeriodeId fordi det var då vilkåret ble vurdert`() {
            val forrigeVilkårperiodeId = UUID.randomUUID()
            val målgruppe = målgruppe(status = Vilkårstatus.UENDRET, forrigeVilkårperiodeId = forrigeVilkårperiodeId)
            assertThat(målgruppe.kopierTilBehandling(behandlingId).forrigeVilkårperiodeId).isEqualTo(forrigeVilkårperiodeId)
        }

        @Test
        fun `kopiering av et vilkår med status ENDRET skal peke til vilkåret sitt id fordi det var då vilkåret ble vurdert`() {
            val forrigeVilkårperiodeId = UUID.randomUUID()
            val målgruppe = målgruppe(status = Vilkårstatus.ENDRET, forrigeVilkårperiodeId = forrigeVilkårperiodeId)
            assertThat(målgruppe.kopierTilBehandling(behandlingId).forrigeVilkårperiodeId).isEqualTo(målgruppe.id)
        }

        @Test
        fun `kopiering av et vilkår med status NY skal peke til vilkåret sitt id fordi det var då vilkåret ble vurdert`() {
            val forrigeVilkårperiodeId = UUID.randomUUID()
            val målgruppe = målgruppe(status = Vilkårstatus.NY, forrigeVilkårperiodeId = forrigeVilkårperiodeId)
            assertThat(målgruppe.kopierTilBehandling(behandlingId).forrigeVilkårperiodeId).isEqualTo(målgruppe.id)
        }
    }
}
