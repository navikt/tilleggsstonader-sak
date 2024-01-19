package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.medlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class VilkårperiodeDtoTest {

    @Test
    fun `skal validere at periode er gyldig`() {
        assertThatThrownBy {
            målgruppe(
                fom = LocalDate.now(),
                tom = LocalDate.now().minusDays(1),
            ).tilDto()
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }

    @Nested
    inner class MappingAvVurdering {

        @ParameterizedTest
        @EnumSource(
            value = ResultatDelvilkårperiode::class,
            names = ["IKKE_AKTUELT"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal returnere delviljår hvis resultat != IKKE_AKTUELT`(resultat: ResultatDelvilkårperiode) {
            val målgruppe = målgruppe(
                delvilkår = DelvilkårMålgruppe(
                    medlemskap = DelvilkårVilkårperiode.Vurdering(
                        svar = null,
                        begrunnelse = null,
                        resultat = resultat,
                    ),
                ),
            ).tilDto()
            assertThat(målgruppe.medlemskap).isNotNull()
            assertThat(målgruppe.medlemskap?.svar).isNull()
        }

        @Test
        fun `skal returnere delvilkårsresultat IKKE_AKTUELT som vurdering=null`() {
            val målgruppe = målgruppe(
                delvilkår = DelvilkårMålgruppe(
                    medlemskap = DelvilkårVilkårperiode.Vurdering(
                        svar = null,
                        begrunnelse = null,
                        resultat = ResultatDelvilkårperiode.IKKE_AKTUELT,
                    ),
                ),
            ).tilDto()
            assertThat(målgruppe.medlemskap).isNull()
        }
    }
}
