package no.nav.tilleggsstonader.sak.vedtak.vedtakOversikt

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class VedtaksperiodeOversiktDto(
    val tilsynBarn: List<VedtaksperiodeOversiktTilsynBarnDto>,
    val læremidler: List<VedtaksperiodeOversiktLæremidlerDto>,
)

data class VedtaksperiodeOversiktTilsynBarnDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallBarn: Int,
    val totalMånedsUtgift: Int,
)

data class VedtaksperiodeOversiktLæremidlerDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val månedsbeløp: Int,
)
