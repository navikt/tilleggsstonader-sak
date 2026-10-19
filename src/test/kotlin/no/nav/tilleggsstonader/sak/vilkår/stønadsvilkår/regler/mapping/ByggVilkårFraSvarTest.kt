package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseOppstartAvslutningHjemreiseRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ByggVilkårFraSvarTest {
    @Test
    fun `svar som ikke er satt skal ikke legges inn som en vurdering`() {
        val svar =
            mapOf(
                RegelId.AVSTAND_OVER_SEKS_KM to
                    SvarOgBegrunnelse(
                        svar = SvarId.JA,
                        begrunnelse = "antall km",
                    ),
                RegelId.UNNTAK_SEKS_KM to null,
                RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to
                    SvarOgBegrunnelse(
                        svar = SvarId.JA,
                    ),
                RegelId.KAN_KJØRE_MED_EGEN_BIL to null,
            )

        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = DagligReiseRegel(),
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
                        svar = SvarId.NEI,
                        begrunnelse = "antall km",
                    ),
                RegelId.UNNTAK_SEKS_KM to SvarOgBegrunnelse(svar = SvarId.JA, begrunnelse = "begrunnelse"),
                RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to
                    SvarOgBegrunnelse(
                        svar = SvarId.NEI,
                        begrunnelse = "begrunnelse",
                    ),
                RegelId.KAN_KJØRE_MED_EGEN_BIL to SvarOgBegrunnelse(SvarId.NEI, "Begrunnelse"),
                RegelId.KAN_REISE_MED_TAXI to SvarOgBegrunnelse(SvarId.NEI, "Begrunnelse"),
            )

        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = DagligReiseRegel(),
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
                        svar = SvarId.JA,
                        begrunnelse = "antall km",
                    ),
                // Unntak skal ikke besvares dersom avstand er over 6 km
                RegelId.UNNTAK_SEKS_KM to SvarOgBegrunnelse(svar = SvarId.JA),
            )

        Assertions
            .assertThatThrownBy {
                ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                    vilkårsregel = DagligReiseRegel(),
                    svar = svar,
                )
            }.hasMessageContaining("Ikke alle svar kunne mappes til vurderinger")
    }

    @Test
    fun `skal kaste feil dersom oppstart inneholder svar som kun er relevante for hjemreise`() {
        val svar =
            mapOf(
                RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL to SvarOgBegrunnelse(svar = SvarId.JA),
            )

        Assertions
            .assertThatThrownBy {
                ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                    vilkårsregel = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.OPPSTART),
                    svar = svar,
                )
            }.hasMessageContaining("Ikke alle svar kunne mappes til vurderinger")
    }

    @Test
    fun `skal kreve begrunnelse når oppstart avslås`() {
        val svar =
            mapOf(
                RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelse(svar = SvarId.NEI),
            )

        Assertions
            .assertThatThrownBy {
                ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                    vilkårsregel = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.OPPSTART),
                    svar = svar,
                )
            }.hasMessageContaining("Påkrevd begrunnelse for regelId=ROAH_HAR_UTGIFTER_TIL_REISEN")
    }

    @Test
    fun `skal kreve begrunnelse når hjemreise avslås i hjemreisebehovsspørsmål`() {
        val svar =
            mapOf(
                RegelId.ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL to SvarOgBegrunnelse(svar = SvarId.NEI),
                RegelId.ROAH_HJEMREISE_BEHOV_FLERE_REISER to SvarOgBegrunnelse(svar = SvarId.NEI),
            )

        Assertions
            .assertThatThrownBy {
                ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                    vilkårsregel = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.HJEMREISE),
                    svar = svar,
                )
            }.hasMessageContaining("Påkrevd begrunnelse for regelId=ROAH_HJEMREISE_BEHOV_FLERE_REISER")
    }

    @Test
    fun `skal kreve begrunnelse når nødvendig utgift er nei`() {
        val svar =
            mapOf(
                RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG to SvarOgBegrunnelse(svar = SvarId.NEI),
            )

        Assertions
            .assertThatThrownBy {
                ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                    vilkårsregel = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.OPPSTART),
                    svar = svar,
                )
            }.hasMessageContaining("Påkrevd begrunnelse for regelId=ROAH_ER_HELE_UTGIFTEN_NØDVENDIG")
    }

    @Test
    fun `skal gi oppfylt resultat når nødvendig utgift er nei med begrunnelse`() {
        val svar =
            mapOf(
                RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN to SvarOgBegrunnelse(svar = SvarId.JA),
                RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG to SvarOgBegrunnelse(svar = SvarId.NEI, begrunnelse = "Delvis privat formål"),
            )

        val delvilkårsett =
            ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
                vilkårsregel = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.OPPSTART),
                svar = svar,
            )

        Assertions.assertThat(delvilkårsett).hasSize(1)
        Assertions.assertThat(delvilkårsett.single().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }
}
