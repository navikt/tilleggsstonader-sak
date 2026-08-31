package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

fun BeregningsresultatReiseTilSamling.mapTilAndelTilkentYtelse(saksbehandling: Saksbehandling): List<AndelTilkjentYtelse> =
    offentligTransport.map { beregningsresultatOffentligTransport ->
        beregningsresultatOffentligTransport.mapTilAndelTilkjentYtelse(saksbehandling)
    } +
        privatBil.map { beregningsresultatPrivatBil ->
            beregningsresultatPrivatBil.mapTilAndelTilkjentYtelse(saksbehandling)
        }

fun BeregningsresultatOffentligTransport.mapTilAndelTilkjentYtelse(saksbehandling: Saksbehandling): AndelTilkjentYtelse {
    val målgrupper = grunnlag.vedtaksperioder.map { it.målgruppe }

    return lagAndelForReiseTilSamling(
        saksbehandling = saksbehandling,
        fomUkedag = grunnlag.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
        beløp = beløp.toInt(),
        målgruppe = målgrupper.singleOrNull() ?: error("Forventet nøyaktig én målgruppe, fant ${målgrupper.size}"),
        brukersNavKontor = grunnlag.brukersNavKontor,
        reiseId = reiseId,
    )
}

fun BeregningsresultatPrivatBil.mapTilAndelTilkjentYtelse(saksbehandling: Saksbehandling): AndelTilkjentYtelse {
    val målgrupper = grunnlag.vedtaksperioder.map { it.målgruppe }

    return lagAndelForReiseTilSamling(
        saksbehandling = saksbehandling,
        fomUkedag = grunnlag.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
        beløp = beløp.toInt(),
        målgruppe = målgrupper.singleOrNull() ?: error("Forventet nøyaktig én målgruppe, fant ${målgrupper.size}"),
        brukersNavKontor = grunnlag.brukersNavKontor,
        reiseId = reiseId,
    )
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

            Stønadstype.REISE_TIL_SAMLING_TSR -> {
                TODO("Har ikke satt opp reise til samling TSR")
            }

            else -> {
                error("Uforventet stønadstype ${saksbehandling.stønadstype}")
            }
        }

    validerBrukersNavKontorForStønadstype(brukersNavKontor, saksbehandling.stønadstype)

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

private fun validerBrukersNavKontorForStønadstype(
    brukersNavKontor: String?,
    stønadstype: Stønadstype,
) {
    when (stønadstype) {
        Stønadstype.REISE_TIL_SAMLING_TSR -> {
            require(!brukersNavKontor.isNullOrBlank()) {
                "Brukers NAV-kontor må være satt for stønadstype $stønadstype"
            }
        }

        Stønadstype.REISE_TIL_SAMLING_TSO -> {
            require(brukersNavKontor == null) {
                "Brukers NAV-kontor skal ikke være satt for stønadstype $stønadstype"
            }
        }

        else -> {}
    }
}

fun finnPeriodeFraAndel(
    beregningsresultat: BeregningsresultatReiseTilSamling,
    andelTilkjentYtelse: AndelTilkjentYtelse,
): Datoperiode {
    val offentligTransportPeriodeMedSammeDatoSomAndel =
        beregningsresultat.offentligTransport.filter {
            it.grunnlag.fom.datoEllerNesteMandagHvisLørdagEllerSøndag() ==
                andelTilkjentYtelse.fom
        }
    val privatBilPeriodeMedSammeDatoSomAndel =
        beregningsresultat.privatBil.filter {
            it.grunnlag.fom.datoEllerNesteMandagHvisLørdagEllerSøndag() ==
                andelTilkjentYtelse.fom
        }

    val alleFom = (
        offentligTransportPeriodeMedSammeDatoSomAndel.map { it.grunnlag.fom } +
            privatBilPeriodeMedSammeDatoSomAndel.map { it.grunnlag.fom }
    )
    val alleTom = (
        offentligTransportPeriodeMedSammeDatoSomAndel.map { it.grunnlag.tom } +
            privatBilPeriodeMedSammeDatoSomAndel.map { it.grunnlag.tom }
    )

    return Datoperiode(
        fom = alleFom.minOrNull()!!,
        tom = alleTom.maxOrNull()!!,
    )
}
