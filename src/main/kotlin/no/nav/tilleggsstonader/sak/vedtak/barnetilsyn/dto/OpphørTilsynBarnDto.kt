package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

data class OpphørTilsynBarnDto(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakTilsynBarnRequest, VedtakTilsynBarnResponse, VedtakTilsynBarnDto(TypeVedtak.OPPHØR)
