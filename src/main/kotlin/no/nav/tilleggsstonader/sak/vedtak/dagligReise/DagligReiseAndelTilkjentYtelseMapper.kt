package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import java.time.LocalDate

fun BeregningsresultatOffentligTransport.mapTilAndelTilkjentYtelse(saksbehandling: Saksbehandling): List<AndelTilkjentYtelse> =
    reiser
        .flatMap { it.perioder }
        .groupBy { it.grunnlag.fom }
        .map { (fom, reiseperioder) ->
            val målgrupper = reiseperioder.flatMap { it.grunnlag.vedtaksperioder }.map { it.målgruppe }
            val typeAktivitet = reiseperioder.flatMap { it.grunnlag.vedtaksperioder }.map { it.typeAktivitet }

            require(målgrupper.distinct().size == 1) {
                "Støtter foreløpig ikke ulike målgrupper på samme utbetalingsdato"
            }

            require(typeAktivitet.distinct().size == 1) {
                "Støtter foreløpig ikke ulike typer aktiviteter på samme utbetalingsdato"
            }

            lagAndelForDagligReise(
                saksbehandling = saksbehandling,
                fomUkedag = fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                beløp = reiseperioder.sumOf { it.beløp },
                målgruppe = målgrupper.first(),
                typeAktivitet = typeAktivitet.first()!!, // TODO ikke !!
            )
        }

private fun lagAndelForDagligReise(
    saksbehandling: Saksbehandling,
    fomUkedag: LocalDate,
    beløp: Int,
    målgruppe: FaktiskMålgruppe,
    typeAktivitet: TypeAktivitet,
): AndelTilkjentYtelse {
    val typeAndel =
        if (saksbehandling.stønadstype == Stønadstype.DAGLIG_REISE_TSO) {
            målgruppe.tilTypeAndel(saksbehandling.stønadstype)
        } else if (saksbehandling.stønadstype == Stønadstype.DAGLIG_REISE_TSR) {
            finnTypeAndelFraTypeAktivitet(typeAktivitet)
        } else {
            error("Uforventet stønadstype ${saksbehandling.stønadstype}")
        }

    return AndelTilkjentYtelse(
        beløp = beløp,
        fom = fomUkedag,
        tom = fomUkedag,
        satstype = Satstype.DAG,
        type = typeAndel,
        kildeBehandlingId = saksbehandling.id,
        utbetalingsdato = fomUkedag,
    )
}

fun finnTypeAndelFraTypeAktivitet(typeAktivitet: TypeAktivitet): TypeAndel =
    when (typeAktivitet) {
        TypeAktivitet.ARBFORB -> TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE
        TypeAktivitet.ARBRRHDAG -> TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB
        TypeAktivitet.ARBTREN -> TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING
        TypeAktivitet.AVKLARAG -> TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING
        TypeAktivitet.DIGIOPPARB -> TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB
        TypeAktivitet.ENKELAMO -> TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO
        TypeAktivitet.ENKFAGYRKE -> TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD
        TypeAktivitet.FORSOPPLEV -> TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET
        TypeAktivitet.GRUPPEAMO -> TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO
        TypeAktivitet.GRUFAGYRKE -> TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD
        TypeAktivitet.HOYEREUTD -> TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING
        TypeAktivitet.INDJOBSTOT -> TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE
        TypeAktivitet.IPSUNG -> TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG
        TypeAktivitet.JOBBK -> TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB
        TypeAktivitet.INDOPPFAG -> TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING
        TypeAktivitet.UTVAOONAV -> TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV
        TypeAktivitet.UTVOPPFOPL -> TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING
        else -> error("Kan ikke mappe til TypeAndel fra TypeAktivitet $typeAktivitet")
    }
