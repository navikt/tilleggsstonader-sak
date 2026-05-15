package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtaksperiodeDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto

fun List<VedtaksperiodeDto>.tilVedtaksperiodeDagligReiseDto() = map { it.tilVedtaksperiodeDagligReiseDto() }

fun VedtaksperiodeDto.tilVedtaksperiodeDagligReiseDto() =
    VedtaksperiodeDagligReiseTsrDto(
        id = id,
        fom = fom,
        tom = tom,
        typeAktivitet = typeAktivitet,
    )
