package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.cucumber.datatable.DataTable
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BeregningNøkler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BoutgifterCucumberNøkler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

fun mapAktiviteter(
    behandlingId: BehandlingId,
    dataTable: DataTable,
) = dataTable.mapRad { rad ->
    aktivitet(
        behandlingId = behandlingId,
        fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
        tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        faktaOgVurdering =
            faktaOgVurderingAktivitetBoutgifter(
                type =
                    parseValgfriEnum<AktivitetType>(BoutgifterCucumberNøkler.AKTIVITET, rad)
                        ?: AktivitetType.TILTAK,
            ),
    )
}

fun mapMålgrupper(
    behandlingId: BehandlingId,
    dataTable: DataTable,
) = dataTable.mapRad { rad ->
    målgruppe(
        behandlingId = behandlingId,
        fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
        tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        faktaOgVurdering =
            faktaOgVurderingMålgruppe(
                type = parseValgfriEnum<MålgruppeType>(BoutgifterCucumberNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
            ),
        begrunnelse = "begrunnelse",
    )
}

fun mapUtgifter(dataTable: DataTable): List<UtgiftBeregningBoutgifter> =
    dataTable.mapRad { rad ->
        UtgiftBeregningBoutgifter(
            fom = parseDato(DomenenøkkelFelles.FOM, rad),
            tom = parseDato(DomenenøkkelFelles.TOM, rad),
            utgift = parseInt(BoutgifterCucumberNøkler.UTGIFT, rad),
        )
    }

fun mapBeregningsresultat(
    dataTable: DataTable,
    utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
) = dataTable.mapRad { rad ->
    val fom = parseDato(DomenenøkkelFelles.FOM, rad)
    val tom = parseDato(DomenenøkkelFelles.TOM, rad)
    BeregningsresultatForLøpendeMåned(
        grunnlag =
            Beregningsgrunnlag(
                fom = fom,
                tom = tom,
                utbetalingsdato = parseDato(BoutgifterCucumberNøkler.UTBETALINGSDATO, rad),
                utgifter = finnRelevanteUtgifter(utgifter = utgifter, fom = fom, tom = tom),
                makssats = parseInt(BoutgifterCucumberNøkler.MAKS_SATS, rad),
                makssatsBekreftet = true,
                målgruppe =
                    parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad)
                        ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet =
                    parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                        ?: AktivitetType.TILTAK,
            ),
    )
}

private fun finnRelevanteUtgifter(
    utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    fom: LocalDate,
    tom: LocalDate,
): Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
    utgifter.mapValues { (_, utgifterListe) -> utgifterListe.filter { it.overlapper(Datoperiode(fom, tom)) } }
