package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class BeregningsresultatLæremidlerDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
    val tidligsteEndring: LocalDate? = null,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val stønadsbeløpPerMåned: Int,
    val stønadsbeløpForPeriode: Int,
    val utbetalingsdato: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val delAvTidligereUtbetaling: Boolean,
) : Periode<LocalDate> {
    fun slåSammen(nestePeriode: BeregningsresultatForPeriodeDto): BeregningsresultatForPeriodeDto =
        this.copy(
            tom = nestePeriode.tom,
            stønadsbeløpForPeriode = this.stønadsbeløpForPeriode + nestePeriode.stønadsbeløpForPeriode,
            antallMåneder = this.antallMåneder + 1,
        )

    fun kanSlåsSammen(nestePeriode: BeregningsresultatForPeriodeDto): Boolean =
        this.målgruppe == nestePeriode.målgruppe &&
            this.aktivitet == nestePeriode.aktivitet &&
            this.studienivå == nestePeriode.studienivå &&
            this.studieprosent == nestePeriode.studieprosent &&
            this.stønadsbeløpPerMåned == nestePeriode.stønadsbeløpPerMåned &&
            this.utbetalingsdato == nestePeriode.utbetalingsdato &&
            this.delAvTidligereUtbetaling == nestePeriode.delAvTidligereUtbetaling &&
            this.påfølgesAv(nestePeriode)
}

fun BeregningsresultatLæremidler.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatLæremidlerDto {
    val perioderDto =
        this
            .filtrerFraOgMed(tidligsteEndring)
            .perioder
            .map { it.tilDto() }
    return BeregningsresultatLæremidlerDto(
        perioder =
            perioderDto
                .mergeSammenhengende(
                    skalMerges = { v1, v2 -> v1.kanSlåsSammen(v2) },
                    merge = { v1, v2 -> v1.slåSammen(v2) },
                ),
        tidligsteEndring = tidligsteEndring,
    )
}

fun BeregningsresultatForMåned.tilDto(): BeregningsresultatForPeriodeDto =
    BeregningsresultatForPeriodeDto(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        antallMåneder = 1,
        studienivå = grunnlag.studienivå,
        studieprosent = grunnlag.studieprosent,
        stønadsbeløpPerMåned = beløp,
        stønadsbeløpForPeriode = beløp,
        utbetalingsdato = grunnlag.utbetalingsdato,
        målgruppe = grunnlag.målgruppe,
        aktivitet = grunnlag.aktivitet,
        delAvTidligereUtbetaling = delAvTidligereUtbetaling,
    )
