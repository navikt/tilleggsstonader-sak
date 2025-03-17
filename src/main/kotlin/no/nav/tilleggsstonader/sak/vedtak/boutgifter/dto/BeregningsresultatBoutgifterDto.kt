package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

data class BeregningsresultatBoutgifterDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallMåneder: Int,
    val stønadsbeløp: Int,
    val utbetalingsdato: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val delAvTidligereUtbetaling: Boolean,
) : Periode<LocalDate> {
    fun slåSammen(nestePeriode: BeregningsresultatForPeriodeDto): BeregningsresultatForPeriodeDto =
        this.copy(
            tom = nestePeriode.tom,
            stønadsbeløp = this.stønadsbeløp + nestePeriode.stønadsbeløp,
            antallMåneder = this.antallMåneder + 1,
        )

    fun kanSlåsSammen(nestePeriode: BeregningsresultatForPeriodeDto): Boolean =
        this.målgruppe == nestePeriode.målgruppe &&
            this.aktivitet == nestePeriode.aktivitet &&
            this.stønadsbeløp == nestePeriode.stønadsbeløp &&
            this.utbetalingsdato == nestePeriode.utbetalingsdato &&
            this.delAvTidligereUtbetaling == nestePeriode.delAvTidligereUtbetaling &&
            this.påfølgesAv(nestePeriode)
}

fun BeregningsresultatBoutgifter.tilDto(revurderFra: LocalDate?): BeregningsresultatBoutgifterDto {
    val perioderDto =
        this
            .filtrerFraOgMed(revurderFra)
            .perioder
            .map { it.tilDto() }
    return BeregningsresultatBoutgifterDto(
        perioder =
            perioderDto
                .mergeSammenhengende(
                    skalMerges = { v1, v2 -> v1.kanSlåsSammen(v2) },
                    merge = { v1, v2 -> v1.slåSammen(v2) },
                ),
    )
}

fun BeregningsresultatForMåned.tilDto(): BeregningsresultatForPeriodeDto =
    BeregningsresultatForPeriodeDto(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        antallMåneder = 1,
        stønadsbeløp = stønadsbeløp,
        utbetalingsdato = grunnlag.utbetalingsdato,
        målgruppe = grunnlag.målgruppe,
        aktivitet = grunnlag.aktivitet,
        delAvTidligereUtbetaling = delAvTidligereUtbetaling,
    )
