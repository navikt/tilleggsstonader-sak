package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

data class VedtaksperioderOversikt(
    val tilsynBarn: List<DetaljertVedtaksperiodeTilsynBarn>,
    val læremidler: List<DetaljertVedtaksperiodeLæremidler>,
    val boutgifter: List<DetaljertVedtaksperiodeBoutgifter>,
)
