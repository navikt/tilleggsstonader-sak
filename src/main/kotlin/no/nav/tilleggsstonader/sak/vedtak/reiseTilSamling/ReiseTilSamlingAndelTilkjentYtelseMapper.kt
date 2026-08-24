package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate
import kotlin.collections.groupBy

fun BeregningReiseTilSamling.mapTilAndelTilkentYtelse(saksbehandling: Saksbehandling): List<AndelTilkjentYtelse> =
    offentligTransport.flatMap { beregningsresultatOffentligTransport ->
        beregningsresultatOffentligTransport.mapTilAndelTilkjentYtelse(saksbehandling)
    }
    } + privatBil.flatMap { beregningsresultatPrivatBil ->
        beregningsresultatPrivatBil.mapTilAndelTilkjentYtelse(saksbehandling
    }

fun BeregningsresultatOffentligTransport.mapTilAndelTilkjentYtelse(saksbehandling: Saksbehandling): List<AndelTilkjentYtelse> {
    val målgrupper = grunnlag.vedtaksperioder.map { it.målgruppe }

    return lagAndelForReiseTilSamling(
        saksbehandling = saksbehandling,
        fomUkedag = grunnlag.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
        beløp = beløp.toInt(),
        målgruppe = grunnlag.målgrupper.first(),
        brukersNavKontor = periode.grunnlag.brukersNavKontor,
        reiseId = null,
    ).groupBy { Triple(it.type, it.fom, it.brukersNavKontor) }
        .map { (_, andeler) -> andeler.first().copy(beløp = andeler.sumOf { it.beløp }) }
}
fun BeregningsresultatPrivatBil.mapTilAndelTilkjentYtelse(saksbehandling: Saksbehandling): List<AndelTilkjentYtelse> {
    val målgrupper = grunnlag.vedtaksperioder.map { it.målgruppe }

    return lagAndelForReiseTilSamling(
        saksbehandling = saksbehandling,
        fomUkedag = grunnlag.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
        beløp = beløp.toInt(),
        målgruppe = grunnlag.målgrupper.first(),
        brukersNavKontor = null,
        reiseId = null,
    ).groupBy { Triple(it.type, it.fom, it.brukersNavKontor) }
        .map { (_, andeler) -> andeler.first().copy(beløp = andeler.sumOf { it.beløp }) }
}

private fun lagAndelForReiseTilSamling(
    saksbehandling: Saksbehandling,
    fomUkedag: LocalDate,
    beløp: Int,
    målgruppe: FaktiskMålgruppe,
    brukersNavKontor: String?,
    reiseId: ReiseId?,
): AndelTilkjentYtelse {
    val typeAndel =
        when (saksbehandling.stønadstype) {
            Stønadstype.REISE_TIL_SAMLING_TSO -> {
                målgruppe.tilTypeAndel(saksbehandling.stønadstype)
            }

            //            Stønadstype.REISE_TIL_SAMLING_TSR -> {
            //                feilHvis(tiltaksvariant == null) {
            //                    "Tiltaksvariant skal alltid være satt for Daglig reise TSR. Var $tiltaksvariant"
            //                }
            //                finnTypeAndelFraTiltaksvariant(tiltaksvariant)
            //            }

            else -> {
                error("Uforventet stønadstype ${saksbehandling.stønadstype}")
            }
        }

    //    validerBrukersNavKontorForStønadstype(brukersNavKontor, saksbehandling.stønadstype)

    return AndelTilkjentYtelse(
        beløp = beløp,
        fom = fomUkedag,
        tom = fomUkedag,
        satstype = Satstype.DAG,
        type = typeAndel,
        utbetalingsdato = fomUkedag,
        brukersNavKontor = brukersNavKontor,
        reiseId = reiseId,
    )
}
