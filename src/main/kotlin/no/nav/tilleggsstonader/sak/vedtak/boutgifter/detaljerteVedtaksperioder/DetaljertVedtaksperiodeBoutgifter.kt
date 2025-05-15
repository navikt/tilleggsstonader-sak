package no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

typealias DetaljertVedtaksperiodeBoutgifter = DetaljertVedtaksperiode<DetaljerBoutgifter>

data class DetaljerBoutgifter(
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val erLøpendeUtgift: Boolean,
    val totalUtgiftMåned: Int,
    val stønadsbeløpMnd: Int,
    val utgifterTilOvernatting: List<UtgiftTilOvernatting>? = null,
)

data class UtgiftTilOvernatting(
    val fom: LocalDate,
    val tom: LocalDate,
    val utgift: Int,
    val beløpSomDekkes: Int,
)
