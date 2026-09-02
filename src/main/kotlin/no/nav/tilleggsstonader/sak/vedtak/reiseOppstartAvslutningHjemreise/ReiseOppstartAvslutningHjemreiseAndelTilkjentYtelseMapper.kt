package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvisIkke
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

fun BeregningsresultatOffentligTransport.mapTilAndelTilkjentYtelse(
    saksbehandling: Saksbehandling,
    vedtaksperioder: List<Vedtaksperiode>,
    aktiviteter: List<Vilkårperiode>,
): AndelTilkjentYtelse =
    lagAndelForReiseOppstart(
        saksbehandling = saksbehandling,
        reiseFom = grunnlag.fom,
        beløp = beløp,
        reiseId = reiseId,
        aktivitetId = aktivitetId,
        vedtaksperioder = vedtaksperioder,
        aktiviteter = aktiviteter,
    )

fun BeregningsresultatPrivatBil.mapTilAndelTilkjentYtelse(
    saksbehandling: Saksbehandling,
    vedtaksperioder: List<Vedtaksperiode>,
    aktiviteter: List<Vilkårperiode>,
): AndelTilkjentYtelse =
    lagAndelForReiseOppstart(
        saksbehandling = saksbehandling,
        reiseFom = grunnlag.fom,
        beløp = beløp,
        reiseId = reiseId,
        aktivitetId = aktivitetId,
        vedtaksperioder = vedtaksperioder,
        aktiviteter = aktiviteter,
    )

private fun lagAndelForReiseOppstart(
    saksbehandling: Saksbehandling,
    reiseFom: LocalDate,
    beløp: BigDecimal,
    reiseId: ReiseId,
    aktivitetId: VilkårperiodeGlobalId,
    vedtaksperioder: List<Vedtaksperiode>,
    aktiviteter: List<Vilkårperiode>,
): AndelTilkjentYtelse {
    // Reisen kan vare lenger enn én dag, men utbetales som en dagsats med reisens startdato som betalingsdag.
    val fomUkedag = reiseFom.datoEllerNesteMandagHvisLørdagEllerSøndag()

    val typeAndel =
        finnTypeAndelReiseOppstart(
            saksbehandling = saksbehandling,
            reiseDato = reiseFom,
            aktivitetId = aktivitetId,
            vedtaksperioder = vedtaksperioder,
            aktiviteter = aktiviteter,
        )

    return AndelTilkjentYtelse(
        beløp = beløp.setScale(0, RoundingMode.HALF_UP).toInt(),
        fom = fomUkedag,
        tom = fomUkedag,
        satstype = Satstype.DAG,
        type = typeAndel,
        utbetalingsdato = fomUkedag,
        reiseId = reiseId,
    )
}

private fun finnTypeAndelReiseOppstart(
    saksbehandling: Saksbehandling,
    reiseDato: LocalDate,
    aktivitetId: VilkårperiodeGlobalId,
    vedtaksperioder: List<Vedtaksperiode>,
    aktiviteter: List<Vilkårperiode>,
): TypeAndel =
    when (saksbehandling.stønadstype) {
        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO -> {
            val målgrupper =
                vedtaksperioder
                    .filter { !reiseDato.isBefore(it.fom) && !reiseDato.isAfter(it.tom) }
                    .map { it.målgruppe }
                    .distinct()

            feilHvisIkke(målgrupper.size == 1) {
                "Forventer nøyaktig én målgruppe for reisedato $reiseDato, fant $målgrupper"
            }
            målgrupper.single().tilTypeAndel(saksbehandling.stønadstype)
        }

        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR -> {
            val aktivitet =
                aktiviteter.find { it.globalId == aktivitetId }
                    ?: error("Finner ikke aktivitet med id=$aktivitetId")
            val tiltaksvariant =
                aktivitet.tiltaksvariant
                    ?: error("Tiltaksvariant må være satt for aktivitet ${aktivitet.globalId}")
            finnTypeAndelFraTiltaksvariantReiseOppstart(tiltaksvariant)
        }

        else -> error("Uforventet stønadstype ${saksbehandling.stønadstype} for reise oppstart/avslutning/hjemreise")
    }

val tiltaksvariantTilTypeAndelReiseOppstartMap =
    mapOf(
        TypeAktivitet.ARBFORB to TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSFORBEREDENDE,
        TypeAktivitet.ARBTREN to TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSTRENING,
        TypeAktivitet.AVKLARAG to TypeAndel.REISE_OPPSTART_TILTAK_AVKLARING,
        TypeAktivitet.ENKELAMO to TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_AMO,
        TypeAktivitet.ENKFAGYRKE to TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
        TypeAktivitet.GRUPPEAMO to TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_AMO,
        TypeAktivitet.GRUFAGYRKE to TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
        TypeAktivitet.HOYEREUTD to TypeAndel.REISE_OPPSTART_TILTAK_HØYERE_UTDANNING,
        TypeAktivitet.JOBBK to TypeAndel.REISE_OPPSTART_TILTAK_JOBBKLUBB,
        TypeAktivitet.INDOPPFAG to TypeAndel.REISE_OPPSTART_TILTAK_OPPFØLGING,
    )

fun finnTypeAndelFraTiltaksvariantReiseOppstart(tiltaksvariant: TypeAktivitet): TypeAndel =
    tiltaksvariantTilTypeAndelReiseOppstartMap[tiltaksvariant]
        ?: feil(
            "Tiltaksvariant ${tiltaksvariant.name} (${tiltaksvariant.beskrivelse}) er ikke støttet for " +
                "innvilgelse av reise oppstart/avslutning/hjemreise TSR. Ta kontakt med utviklerteamet.",
        )
