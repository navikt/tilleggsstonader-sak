package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak

enum class BehandlingÅrsakDvh {
    KLAGE,
    NYE_OPPLYSNINGER,
    SØKNAD,
    PAPIRSØKNAD,
    MANUELT_OPPRETTET,
    MANUELT_OPPRETTET_UTEN_BREV,
    KORRIGERING_UTEN_BREV,
    SATSENDRING,
    ;

    companion object {
        fun fraDomene(årsak: BehandlingÅrsak) = when (årsak) {
            BehandlingÅrsak.KLAGE -> KLAGE
            BehandlingÅrsak.NYE_OPPLYSNINGER -> NYE_OPPLYSNINGER
            BehandlingÅrsak.SØKNAD -> SØKNAD
            BehandlingÅrsak.PAPIRSØKNAD -> PAPIRSØKNAD
            BehandlingÅrsak.MANUELT_OPPRETTET -> MANUELT_OPPRETTET
            BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV -> MANUELT_OPPRETTET_UTEN_BREV
            BehandlingÅrsak.KORRIGERING_UTEN_BREV -> KORRIGERING_UTEN_BREV
            BehandlingÅrsak.SATSENDRING -> SATSENDRING
        }
    }
}
