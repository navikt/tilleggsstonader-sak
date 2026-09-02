package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import java.math.BigDecimal
import java.time.LocalDate

data class DetaljertVedtaksperiodeReiseTilSamling(
    val stønadstype: Stønadstype,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beløp: BigDecimal,
) : DetaljertVedtaksperiode,
    Periode<LocalDate>
