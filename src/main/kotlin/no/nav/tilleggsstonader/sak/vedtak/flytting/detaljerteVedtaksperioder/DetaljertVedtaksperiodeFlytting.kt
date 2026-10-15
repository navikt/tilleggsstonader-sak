package no.nav.tilleggsstonader.sak.vedtak.flytting.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlytteSelv
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.Flyttemåte
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.skalUtbetales
import java.math.BigDecimal
import java.time.LocalDate

data class DetaljertVedtaksperiodeFlytting(
    val stønadstype: Stønadstype,
    val flyttedato: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val beløp: BigDecimal,
    val flyttemåte: Flyttemåte,
    val skalUtbetales: Boolean,
) : DetaljertVedtaksperiode

fun BeregningsresultatFlytting.tilDetaljerteVedtaksperioder(stønadstype: Stønadstype): List<DetaljertVedtaksperiodeFlytting> =
    resultater
        .map { resultat ->
            DetaljertVedtaksperiodeFlytting(
                stønadstype = stønadstype,
                flyttedato = resultat.fom,
                målgruppe = resultat.målgruppe,
                beløp = resultat.beløp,
                flyttemåte =
                    when (resultat.grunnlag) {
                        is BeregningsgrunnlagFlytteSelv -> Flyttemåte.FLYTTE_SELV
                        is BeregningsgrunnlagFlyttebyrå -> Flyttemåte.FLYTTEBYRÅ
                    },
                skalUtbetales = resultat.skalUtbetales(),
            )
        }.sortedBy { it.flyttedato }
