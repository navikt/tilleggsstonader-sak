package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class VedtaksperiodeOversiktDto(
    val tilsynBarn: List<DetaljertVedtaksperiodeTilsynBarnDto>,
    val læremidler: List<DetaljertVedtaksperiodeLæremidlerDto>,
)

data class DetaljertVedtaksperiodeTilsynBarnDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallBarn: Int,
    val totalMånedsUtgift: Int,
)

data class DetaljertVedtaksperiodeLæremidlerDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val månedsbeløp: Int,
)

fun VedtaksperioderOversikt.tilDto(): VedtaksperiodeOversiktDto =
    VedtaksperiodeOversiktDto(
        tilsynBarn = tilsynBarn.map { it -> it.tilDto() },
        læremidler = læremidler.map { it -> it.tilDto() },
    )

private fun DetaljertVedtaksperiodeTilsynBarn.tilDto() =
    DetaljertVedtaksperiodeTilsynBarnDto(
        fom = this.fom,
        tom = this.tom,
        aktivitet = this.aktivitet,
        målgruppe = this.målgruppe,
        antallBarn = this.antallBarn,
        totalMånedsUtgift = this.totalMånedsUtgift,
    )

private fun DetaljertVedtaksperiodeLæremidler.tilDto() =
    DetaljertVedtaksperiodeLæremidlerDto(
        fom = this.fom,
        tom = this.tom,
        aktivitet = this.aktivitet,
        målgruppe = this.målgruppe,
        antallMåneder = this.antallMåneder,
        studienivå = this.studienivå,
        studieprosent = this.studieprosent,
        månedsbeløp = this.månedsbeløp,
    )
