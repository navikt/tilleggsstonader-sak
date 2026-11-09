package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType

/**
 * Hvilken type reise vilkåret gjelder for (oppstart, avslutning eller hjemreise). Lagres som eget fakta-felt,
 * satt direkte av saksbehandler/frontend. Brukes for å velge riktig flyt i [ReiseOppstartAvslutningHjemreiseRegel].
 **/
enum class TypeReiseformål {
    OPPSTART,
    AVSLUTNING,
    HJEMREISE,
}

fun TypeReiseformål.tilVilkårType() =
    when (this) {
        TypeReiseformål.OPPSTART, TypeReiseformål.AVSLUTNING -> VilkårType.ROAH_OPPSTART_AVSLUTNING
        TypeReiseformål.HJEMREISE -> VilkårType.ROAH_HJEMREISE
    }
