package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

/**
 * Hvilken type reise vilkåret gjelder for (oppstart, avslutning eller hjemreise). Lagres som eget fakta-felt,
 * satt direkte av saksbehandler/frontend – er ikke en del av selve regelverket ([ReiseOppstartAvslutningHjemreiseRegel]).
 */
enum class TypeReiseformål {
    OPPSTART,
    AVSLUTNING,
    HJEMREISE,
}
