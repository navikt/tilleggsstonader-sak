package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatPrivatBil(
    val reiser: List<BeregningsresultatForReisePrivatBil>,
)

data class BeregningsresultatForReisePrivatBil(
    val reiseId: ReiseId,
    val perioder: List<BeregningsresultatForReisePrivatBilPeriode>,
)

data class BeregningsresultatForReisePrivatBilPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalingsdato: LocalDate,
    val grunnlag: BeregningsresultatForReisePrivatBilGrunnlag,
    val stønadsbeløp: BigDecimal,
)

data class BeregningsresultatForReisePrivatBilGrunnlag(
    val dager: List<BeregningsresultatForReisePrivatBilDag>,
    val dagsatsUtenParkering: BigDecimal,
)

data class BeregningsresultatForReisePrivatBilDag(
    val dato: LocalDate,
    val parkeringskostnad: Int,
    val stønadsbeløpForDag: BigDecimal,
)
