package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
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
        .flatMap { (fom, reiseperioder) ->
            val målgrupper = reiseperioder.flatMap { it.grunnlag.vedtaksperioder }.map { it.målgruppe }
            val typeAktivitet = reiseperioder.flatMap { it.grunnlag.vedtaksperioder }.map { it.typeAktivitet }

            require(målgrupper.distinct().size == 1) {
                "Støtter foreløpig ikke ulike målgrupper på samme utbetalingsdato"
            }

            if (saksbehandling.stønadstype == Stønadstype.DAGLIG_REISE_TSR) {
                require(typeAktivitet.distinct().size == 1) {
                    "Støtter foreløpig ikke ulike typer aktiviteter på samme utbetalingsdato"
                }
            }

            // Grupperer på brukersNavKontor for å ta høyde for at de kan ha ulike kontorer
            reiseperioder
                .groupBy { it.grunnlag.brukersNavKontor }
                .map { (brukersNavKontor, reiseperioderMedSammeBrukersNavKontor) ->
                    lagAndelForDagligReise(
                        saksbehandling = saksbehandling,
                        fomUkedag = fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                        beløp = reiseperioderMedSammeBrukersNavKontor.sumOf { it.beløp },
                        målgruppe = målgrupper.first(),
                        typeAktivitet = typeAktivitet.firstOrNull(),
                        brukersNavKontor = brukersNavKontor,
                    )
                }
        }

private fun lagAndelForDagligReise(
    saksbehandling: Saksbehandling,
    fomUkedag: LocalDate,
    beløp: Int,
    målgruppe: FaktiskMålgruppe,
    typeAktivitet: TypeAktivitet?,
    brukersNavKontor: String?,
): AndelTilkjentYtelse {
    val typeAndel =
        when (saksbehandling.stønadstype) {
            Stønadstype.DAGLIG_REISE_TSO -> {
                målgruppe.tilTypeAndel(saksbehandling.stønadstype)
            }
            Stønadstype.DAGLIG_REISE_TSR -> {
                feilHvis(typeAktivitet == null) {
                    "Variant/Typeaktivitet skal alltid være satt for Daglig Reise Tsr. Var $typeAktivitet"
                }
                finnTypeAndelFraTypeAktivitet(typeAktivitet)
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
        kildeBehandlingId = saksbehandling.id,
        utbetalingsdato = fomUkedag,
        brukersNavKontor = brukersNavKontor,
    )
}

private fun validerBrukersNavKontorForStønadstype(
    brukersNavKontor: String?,
    stønadstype: Stønadstype,
) {
    when (stønadstype) {
        Stønadstype.DAGLIG_REISE_TSR -> {
            require(!brukersNavKontor.isNullOrBlank()) {
                "Brukers NAV-kontor må være satt for stønadstype $stønadstype"
            }
        }
        Stønadstype.DAGLIG_REISE_TSO -> {
            require(brukersNavKontor == null) {
                "Brukers NAV-kontor skal ikke være satt for stønadstype $stønadstype"
            }
        }

        else -> {}
    }
}

val typeAktivitetTilTypeAndelMap =
    mapOf(
        TypeAktivitet.ARBFORB to TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE,
        TypeAktivitet.ARBRRHDAG to TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB,
        TypeAktivitet.ARBTREN to TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING,
        TypeAktivitet.AVKLARAG to TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING,
        TypeAktivitet.DIGIOPPARB to TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB,
        TypeAktivitet.ENKELAMO to TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO,
        TypeAktivitet.ENKFAGYRKE to TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
        TypeAktivitet.FORSOPPLEV to TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET,
        TypeAktivitet.GRUPPEAMO to TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO,
        TypeAktivitet.GRUFAGYRKE to TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
        TypeAktivitet.HOYEREUTD to TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING,
        TypeAktivitet.INDJOBSTOT to TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE,
        TypeAktivitet.IPSUNG to TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG,
        TypeAktivitet.JOBBK to TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB,
        TypeAktivitet.INDOPPFAG to TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING,
        TypeAktivitet.UTVAOONAV to TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV,
        TypeAktivitet.UTVOPPFOPL to TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
    )

fun finnTypeAndelFraTypeAktivitet(typeAktivitet: TypeAktivitet): TypeAndel =
    typeAktivitetTilTypeAndelMap[typeAktivitet]
        ?: error("Kan ikke mappe til TypeAndel fra TypeAktivitet $typeAktivitet")
