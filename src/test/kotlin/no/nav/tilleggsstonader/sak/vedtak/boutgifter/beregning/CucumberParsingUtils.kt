package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.cucumber.datatable.DataTable
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriDato
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BeregningNøkler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BoutgifterDomenenøkkel
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

// Trenger denne fordi det ellers vil bli unik UUID per vedtaksperiode, som gjør at sammenlikningen blir misfornøyd
val vedtaksperiodeId: UUID = UUID.randomUUID()

fun mapUtgifter(dataTable: DataTable): List<UtgiftBeregningBoutgifter> =
    dataTable.mapRad { rad ->
        UtgiftBeregningBoutgifter(
            fom = parseDato(DomenenøkkelFelles.FOM, rad),
            tom = parseDato(DomenenøkkelFelles.TOM, rad),
            utgift = parseInt(BoutgifterDomenenøkkel.UTGIFT, rad),
        )
    }

fun mapVedtaksperioder(dataTable: DataTable): List<Vedtaksperiode> =
    dataTable.mapRad { rad ->
        vedtaksperiode(
            id = vedtaksperiodeId,
            fom = parseDato(DomenenøkkelFelles.FOM, rad),
            tom = parseDato(DomenenøkkelFelles.TOM, rad),
            målgruppe =
                parseValgfriEnum<FaktiskMålgruppe>(BoutgifterDomenenøkkel.MÅLGRUPPE, rad)
                    ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet =
                parseValgfriEnum<AktivitetType>(BoutgifterDomenenøkkel.AKTIVITET, rad)
                    ?: AktivitetType.TILTAK,
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
                utbetalingsdato = parseDato(BoutgifterDomenenøkkel.UTBETALINGSDATO, rad),
                utgifter = finnRelevanteUtgifter(utgifter = utgifter, fom = fom, tom = tom),
                makssats = parseInt(BoutgifterDomenenøkkel.MAKS_SATS, rad),
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

data class ForenkletAndel(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val satstype: Satstype,
    val type: TypeAndel,
    val utbetalingsdato: LocalDate,
    val statusIverksetting: StatusIverksetting,
) {
    constructor(andel: AndelTilkjentYtelse) : this(
        fom = andel.fom,
        tom = andel.tom,
        beløp = andel.beløp,
        satstype = andel.satstype,
        type = andel.type,
        utbetalingsdato = andel.utbetalingsdato,
        statusIverksetting = andel.statusIverksetting,
    )
}

fun mapAndeler(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        ForenkletAndel(
            fom = parseDato(DomenenøkkelFelles.FOM, rad),
            tom = parseValgfriDato(DomenenøkkelFelles.TOM, rad) ?: parseDato(DomenenøkkelFelles.FOM, rad),
            beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
            satstype = parseValgfriEnum<Satstype>(DomenenøkkelAndelTilkjentYtelse.SATS, rad) ?: Satstype.DAG,
            type = parseEnum(DomenenøkkelAndelTilkjentYtelse.TYPE, rad),
            utbetalingsdato = parseDato(DomenenøkkelAndelTilkjentYtelse.UTBETALINGSDATO, rad),
            statusIverksetting =
                parseValgfriEnum<StatusIverksetting>(
                    domenebegrep = DomenenøkkelAndelTilkjentYtelse.STATUS_IVERKSETTING,
                    rad = rad,
                ) ?: StatusIverksetting.UBEHANDLET,
        )
    }
