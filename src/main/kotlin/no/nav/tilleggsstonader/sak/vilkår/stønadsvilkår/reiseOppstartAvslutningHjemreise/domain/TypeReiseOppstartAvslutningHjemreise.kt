package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

/**
 * Hvordan søker reiser (transportmiddel). Speiler [no.nav.tilleggsstonader.sak.vedtak.domain.TypeReiseTilSamling].
 */
enum class TypeReiseOppstartAvslutningHjemreise {
    OFFENTLIG_TRANSPORT,
    PRIVAT_BIL,
    UBESTEMT,
}
