package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.ANNET
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.HAR_IKKE_MERUTGIFTER
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.HAR_IKKE_UTGIFTER
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.IKKE_I_MÅLGRUPPE
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.INGEN_AKTIVITET
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.MANGELFULL_DOKUMENTASJON
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.RETT_TIL_BOSTØTTE
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND

/**
 * Kategorier som beskriver hvilket område en avslagsårsak tilhører
 */
enum class Avslagskategori {
    AKTIVITET,
    MÅLGRUPPE,
    STØNADSVILKÅR,
    GENERELL,
}

fun gyldigeAvslagsårsaker(
    forStønadstype: Stønadstype,
    basertPå: Avslagskategori,
): Set<ÅrsakAvslag> =
    when (forStønadstype) {
        Stønadstype.BARNETILSYN ->
            when (basertPå) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> setOf(MANGELFULL_DOKUMENTASJON)
                Avslagskategori.GENERELL -> setOf(ANNET)
            }

        Stønadstype.LÆREMIDLER ->
            when (basertPå) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET, HAR_IKKE_UTGIFTER, RETT_TIL_UTSTYRSSTIPEND)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> emptySet()
                Avslagskategori.GENERELL -> setOf(ANNET)
            }

        Stønadstype.BOUTGIFTER ->
            when (basertPå) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> setOf(MANGELFULL_DOKUMENTASJON, HAR_IKKE_MERUTGIFTER, RETT_TIL_BOSTØTTE)
                Avslagskategori.GENERELL -> setOf(ANNET, INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE)
            }

        Stønadstype.DAGLIG_REISE_TSO -> // TODO: Denne må trolig fylles inn mer når vi lærer mer om hvordan vilkår på daglig reise ser ut
            when (basertPå) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> emptySet()
                Avslagskategori.GENERELL -> setOf(ANNET, INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE)
            }

        Stønadstype.DAGLIG_REISE_TSR -> // TODO: Denne må trolig fylles inn mer når vi lærer mer om hvordan vilkår på daglig reise ser ut
            when (basertPå) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> emptySet()
                Avslagskategori.GENERELL -> setOf(ANNET, INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE)
            }
    }

fun gyldigeÅrsakerForStønadstype(stønadstype: Stønadstype): Set<ÅrsakAvslag> =
    Avslagskategori.entries
        .flatMap { gyldigeAvslagsårsaker(stønadstype, it) }
        .toSet()
