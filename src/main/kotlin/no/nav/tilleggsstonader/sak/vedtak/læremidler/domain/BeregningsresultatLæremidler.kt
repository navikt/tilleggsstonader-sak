package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

data class BeregningsresultatLæremidler(
    val perioder: List<BeregningsresultatForMåned>,
)

data class BeregningsresultatForMåned(
    val beløp: Int,
    val grunnlag: Beregningsgrunnlag,
)

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
) : Periode<LocalDate>

