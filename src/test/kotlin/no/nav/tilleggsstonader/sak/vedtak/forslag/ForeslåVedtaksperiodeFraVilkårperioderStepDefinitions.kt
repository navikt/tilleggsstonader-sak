package no.nav.tilleggsstonader.sak.vedtak.forslag

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat

@Suppress("ktlint:standard:function-naming", "unused")
class ForeslåVedtaksperiodeFraVilkårperioderStepDefinitions {
    var aktiviteter: List<VilkårperiodeAktivitet> = emptyList()
    var målgrupper: List<VilkårperiodeMålgruppe> = emptyList()
    var resultat: List<ForslagVedtaksperiodeFraVilkårperioder> = emptyList()
    var resultatFaktiskMålgruppe: List<ForslagVedtaksperiodeFraVilkårperioderFaktiskMålgruppe> = emptyList()
    var feil: ApiFeil? = null

    @Gitt("følgende vilkårsperioder med aktiviteter")
    fun `følgende vilkårsperioder med aktiviteter`(dataTable: DataTable) {
        aktiviteter =
            dataTable.mapRad { rad ->
                aktivitet(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetTilsynBarn(
                            type = AktivitetType.valueOf(rad["type"]!!),
                        ),
                )
            }
    }

    @Gitt("følgende vilkårsperioder med målgrupper")
    fun `følgende vilkårsperioder med målgrupper`(dataTable: DataTable) {
        målgrupper =
            dataTable.mapRad { rad ->
                målgruppe(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppe(
                            type = MålgruppeType.valueOf(rad["type"]!!),
                        ),
                    begrunnelse = "begrunnelse",
                )
            }
    }

    @Når("forslag til vedtaksperioder fra vilkårperioder lages")
    fun `forslag til vedtaksperioder lages`() {
        try {
            resultat =
                ForeslåVedtaksperiodeFraVilkårperioder.foreslåVedtaksperioder(
                    Vilkårperioder(
                        målgrupper = målgrupper,
                        aktiviteter = aktiviteter,
                    ),
                )
        } catch (e: ApiFeil) {
            feil = e
        }
    }

    @Når("forslag til vedtaksperioder fra vilkårperioder lages faktisk målgruppe")
    fun `forslag til vedtaksperioder fra vilkårperioder lages faktisk målgruppe`() {
        try {
            resultatFaktiskMålgruppe =
                ForeslåVedtaksperiodeFraVilkårperioder.foreslåVedtaksperioderFaktiskMålgruppe(
                    Vilkårperioder(
                        målgrupper = målgrupper,
                        aktiviteter = aktiviteter,
                    ),
                )
        } catch (e: ApiFeil) {
            feil = e
        }
    }

    @Så("forvent følgende beregningsfeil: {}")
    fun `forvent følgende beregningsfeil`(feil: String) {
        assertThat(this.feil).isNotNull
        assertThat(this.feil?.message).isEqualTo(feil)
    }

    @Så("forvent følgende forslag fra vilkårperioder")
    fun `forvent følgende forslag`(dataTable: DataTable) {
        val forventetStønadsperioder =
            dataTable.mapRad { rad ->
                ForslagVedtaksperiodeFraVilkårperioder(
                    fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                    tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
                    målgruppe = MålgruppeType.valueOf(rad["målgruppe"]!!),
                    aktivitet = AktivitetType.valueOf(rad["aktivitet"]!!),
                )
            }

        assertThat(resultat).isEqualTo(forventetStønadsperioder)
    }

    @Så("forvent følgende forslag fra vilkårperioder faktisk målgruppe")
    fun `forvent følgende forslag faktisk målgruppe`(dataTable: DataTable) {
        val forventetStønadsperioder =
            dataTable.mapRad { rad ->
                ForslagVedtaksperiodeFraVilkårperioderFaktiskMålgruppe(
                    fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                    tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
                    målgruppe = FaktiskMålgruppe.valueOf(rad["målgruppe"]!!),
                    aktivitet = AktivitetType.valueOf(rad["aktivitet"]!!),
                )
            }

        assertThat(resultatFaktiskMålgruppe).isEqualTo(forventetStønadsperioder)
    }
}
