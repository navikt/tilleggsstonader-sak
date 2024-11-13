package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat

enum class TypeVedtak {
    INNVILGELSE,
    AVSLAG,
    OPPHØR,
}

fun TypeVedtak.tilBehandlingResult(): BehandlingResultat = when (this) {
    TypeVedtak.INNVILGELSE -> BehandlingResultat.INNVILGET
    TypeVedtak.AVSLAG -> BehandlingResultat.AVSLÅTT
    TypeVedtak.OPPHØR -> BehandlingResultat.OPPHØRT
}
