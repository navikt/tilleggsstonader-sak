package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeTsrDto

fun List<VedtaksperiodeDto>.tilVedtaksperiodeTsrDto() = map { it.tilVedtaksperiodeTsrDto() }

fun VedtaksperiodeDto.tilVedtaksperiodeTsrDto() =
    VedtaksperiodeTsrDto(
        id = id,
        fom = fom,
        tom = tom,
    )
