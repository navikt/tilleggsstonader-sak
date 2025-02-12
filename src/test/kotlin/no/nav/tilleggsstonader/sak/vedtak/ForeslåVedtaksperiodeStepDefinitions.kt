package no.nav.tilleggsstonader.sak.vedtak

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
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

enum class DomenenøkkelForeslåVedtaksperioder(
    override val nøkkel: String,
) : Domenenøkkel {
    RESULTAT("Resultat"),
    TYPE("type"),
    AKTIVITET("aktivitet"),
    MÅLGRUPPE("målgruppe"),
}

@Suppress("ktlint:standard:function-naming", "unused")
class ForeslåVedtaksperiodeStepDefinitions {
    var aktiviteter: List<VilkårperiodeAktivitet> = emptyList()
    var målgrupper: List<VilkårperiodeMålgruppe> = emptyList()
    var vilkår: List<Vilkår> = emptyList()
    var resultat: List<Vedtaksperiode> = emptyList()
    var feil: ApiFeil? = null

    @Gitt("følgende vilkårsperioder med aktiviteter for vedtaksforslag")
    fun `følgende vilkårsperioder med aktiviteter`(dataTable: DataTable) {
        aktiviteter = mapAktiviteter(dataTable)
    }

    @Gitt("følgende vilkårsperioder med målgrupper for vedtaksforslag")
    fun `følgende vilkårsperioder med målgrupper`(dataTable: DataTable) {
        målgrupper = mapMålgruppe(dataTable)
    }

    @Gitt("følgende vilkår for vedtaksforslag")
    fun `følgende vilkår`(dataTable: DataTable) {
        vilkår = mapVilkår(dataTable)
    }

    @Når("forslag til vedtaksperioder lages")
    fun `forslag til vedtaksperioder lages`() {
        try {
            resultat =
                ForeslåVedtaksperiode.finnVedtaksperiode(
                    Vilkårperioder(
                        målgrupper = målgrupper,
                        aktiviteter = aktiviteter,
                    ),
                    vilkår,
                )
        } catch (e: ApiFeil) {
            feil = e
        }
    }

    @Så("forvent følgende feil for vedtaksforsalg: {}")
    fun `forvent følgende feil`(feil: String) {
        assertThat(this.feil).isNotNull
        assertThat(this.feil?.message).isEqualTo(feil)
    }

    @Så("forvent følgende vedtaksperioder")
    fun `forvent følgende vedtaksperioder`(dataTable: DataTable) {
        val forventetVedtaksperioder = mapVedtaksperioder(dataTable)
        assertThat(resultat).isEqualTo(forventetVedtaksperioder)
    }

    private fun mapAktiviteter(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            aktivitet(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering =
                    faktaOgVurderingAktivitetTilsynBarn(
                        type = parseValgfriEnum<AktivitetType>(DomenenøkkelForeslåVedtaksperioder.TYPE, rad)!!,
                    ),
            )
        }

    private fun mapMålgruppe(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            målgruppe(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering =
                    faktaOgVurderingMålgruppe(
                        type = parseValgfriEnum<MålgruppeType>(DomenenøkkelForeslåVedtaksperioder.TYPE, rad)!!,
                    ),
            )
        }

    private fun mapVilkår(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            vilkår(
                behandlingId = BehandlingId.random(),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                resultat = parseValgfriEnum<Vilkårsresultat>(DomenenøkkelForeslåVedtaksperioder.RESULTAT, rad)!!,
                type = VilkårType.PASS_BARN,
            )
        }

    private fun mapVedtaksperioder(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            Vedtaksperiode(
                fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
                målgruppe = parseValgfriEnum<MålgruppeType>(DomenenøkkelForeslåVedtaksperioder.MÅLGRUPPE, rad)!!,
                aktivitet = parseValgfriEnum<AktivitetType>(DomenenøkkelForeslåVedtaksperioder.AKTIVITET, rad)!!,
            )
        }
}
