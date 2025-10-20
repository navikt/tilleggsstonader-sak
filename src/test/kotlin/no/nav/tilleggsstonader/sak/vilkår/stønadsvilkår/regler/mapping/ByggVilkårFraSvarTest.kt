package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseOffentiligTransportRegel
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ByggVilkårFraSvarTest {
    @Test
    fun `svar som ikke er satt skal ikke legges inn som en vurdering`() {
        val svar =
            mapOf(
                RegelId.AVSTAND_OVER_SEKS_KM to
                    SvarOgBegrunnelse(
                        svarId = SvarId.JA,
                    ),
                RegelId.UNNTAK_SEKS_KM to null,
                RegelId.KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT to
                    SvarOgBegrunnelse(
                        svarId = SvarId.JA,
                    ),
                RegelId.KAN_BRUKER_KJØRE_SELV to null,
            )

        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = DagligReiseOffentiligTransportRegel(),
                svar = svar,
            )

        Assertions.assertThat(delvilkårsett).hasSize(1)
        Assertions.assertThat(delvilkårsett[0].vurderinger).hasSize(2)
        Assertions.assertThat(delvilkårsett[0].resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `om alle regler er besvart skal alle legges inn som vurderinger`() {
        val svar =
            mapOf(
                RegelId.AVSTAND_OVER_SEKS_KM to
                    SvarOgBegrunnelse(
                        svarId = SvarId.NEI,
                    ),
                RegelId.UNNTAK_SEKS_KM to SvarOgBegrunnelse(svarId = SvarId.JA),
                RegelId.KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT to
                    SvarOgBegrunnelse(
                        svarId = SvarId.NEI,
                    ),
                RegelId.KAN_BRUKER_KJØRE_SELV to SvarOgBegrunnelse(SvarId.NEI, "Begrunnelse"),
            )

        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = DagligReiseOffentiligTransportRegel(),
                svar = svar,
            )

        Assertions.assertThat(delvilkårsett).hasSize(1)
        Assertions.assertThat(delvilkårsett[0].vurderinger).hasSize(svar.size)
    }

    @Test
    fun `skal kaste feil dersom et regler som ikke var relevante er besvart`() {
        val svar =
            mapOf(
                RegelId.AVSTAND_OVER_SEKS_KM to
                    SvarOgBegrunnelse(
                        svarId = SvarId.JA,
                    ),
                // Unntak skal ikke besvares dersom avstand er over 6 km
                RegelId.UNNTAK_SEKS_KM to SvarOgBegrunnelse(svarId = SvarId.JA),
            )

        Assertions
            .assertThatThrownBy {
                ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                    vilkårsregel = DagligReiseOffentiligTransportRegel(),
                    svar = svar,
                )
            }.hasMessageContaining("Ikke alle svar kunne mappes til vurderinger")
    }
}
