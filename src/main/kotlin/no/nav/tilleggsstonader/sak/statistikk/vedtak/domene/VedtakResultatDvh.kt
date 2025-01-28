package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat

enum class VedtakResultatDvh {
    INNVILGET,
    AVSLÅTT,
    OPPHØRT,
    ;

    companion object {
        fun fraDomene(behandlingResultat: BehandlingResultat): VedtakResultatDvh =
            when (behandlingResultat) {
                BehandlingResultat.INNVILGET -> INNVILGET
                BehandlingResultat.OPPHØRT -> OPPHØRT
                BehandlingResultat.AVSLÅTT -> AVSLÅTT
                BehandlingResultat.IKKE_SATT, BehandlingResultat.HENLAGT ->
                    error("Skal ikke sende vedtaksstatistikk når resultat=$behandlingResultat.")
            }
    }
}
