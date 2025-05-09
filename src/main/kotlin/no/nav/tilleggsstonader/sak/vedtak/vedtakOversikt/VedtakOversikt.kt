package no.nav.tilleggsstonader.sak.vedtak.vedtakOversikt

data class VedtaksperiodeOversikt(
    val tilsynBarn: List<DetaljertVedtaksperiodeTilsynBarn>,
    val læremidler: List<DetaljertVedtaksperiodeLæremidler>,
)
