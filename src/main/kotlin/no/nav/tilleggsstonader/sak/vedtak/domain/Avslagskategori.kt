package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.ANNET
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.HAR_IKKE_MERUTGIFTER
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.HAR_IKKE_UTGIFTER
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.IKKE_I_MÅLGRUPPE
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.INGEN_AKTIVITET
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.LØNN_I_TILTAK
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.MANGELFULL_DOKUMENTASJON
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag.REISEAVSTAND_UNDER_6_KM
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
    stønadstype: Stønadstype,
    gjelder: Avslagskategori,
): Set<ÅrsakAvslag> {
    val generelleÅrsaker = setOf(ANNET, INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE)
    return when (stønadstype) {
        Stønadstype.BARNETILSYN ->
            when (gjelder) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET, LØNN_I_TILTAK)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> setOf(MANGELFULL_DOKUMENTASJON)
                Avslagskategori.GENERELL -> generelleÅrsaker
            }

        Stønadstype.LÆREMIDLER ->
            when (gjelder) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET, HAR_IKKE_UTGIFTER, RETT_TIL_UTSTYRSSTIPEND)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> emptySet()
                Avslagskategori.GENERELL -> generelleÅrsaker
            }

        Stønadstype.BOUTGIFTER ->
            when (gjelder) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET, LØNN_I_TILTAK)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> setOf(MANGELFULL_DOKUMENTASJON, HAR_IKKE_MERUTGIFTER, RETT_TIL_BOSTØTTE, LØNN_I_TILTAK)
                Avslagskategori.GENERELL -> generelleÅrsaker
            }

        Stønadstype.DAGLIG_REISE_TSO ->
            when (gjelder) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET, LØNN_I_TILTAK)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR -> setOf(MANGELFULL_DOKUMENTASJON, REISEAVSTAND_UNDER_6_KM, LØNN_I_TILTAK)
                Avslagskategori.GENERELL -> generelleÅrsaker
            }

        Stønadstype.DAGLIG_REISE_TSR ->
            when (gjelder) {
                Avslagskategori.AKTIVITET -> setOf(INGEN_AKTIVITET)
                Avslagskategori.MÅLGRUPPE -> setOf(IKKE_I_MÅLGRUPPE)
                Avslagskategori.STØNADSVILKÅR ->
                    setOf(
                        MANGELFULL_DOKUMENTASJON,
                        REISEAVSTAND_UNDER_6_KM,
                    )
                Avslagskategori.GENERELL -> generelleÅrsaker
            }
    }
}

fun gyldigeÅrsakerForStønadstype(stønadstype: Stønadstype): Set<ÅrsakAvslag> =
    Avslagskategori.entries
        .flatMap { gyldigeAvslagsårsaker(stønadstype, it) }
        .toSet()
