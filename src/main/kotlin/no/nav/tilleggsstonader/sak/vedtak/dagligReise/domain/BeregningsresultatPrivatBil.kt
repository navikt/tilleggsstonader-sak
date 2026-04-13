package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
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
    override val fom: LocalDate,
    override val tom: LocalDate,
    val grunnlag: BeregningsresultatForReisePrivatBilGrunnlag,
    val stønadsbeløp: BigDecimal,
    val brukersNavKontor: String?,
) : Periode<LocalDate>

data class BeregningsresultatForReisePrivatBilGrunnlag(
    val dager: List<BeregningsresultatForReisePrivatBilDag>,
)

data class BeregningsresultatForReisePrivatBilDag(
    val dato: LocalDate,
    val parkeringskostnad: Int,
    val dagsatsUtenParkering: BigDecimal,
    val stønadsbeløpForDag: BigDecimal,
)
