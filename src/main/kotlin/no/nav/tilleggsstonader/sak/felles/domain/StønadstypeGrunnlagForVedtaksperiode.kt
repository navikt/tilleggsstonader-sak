package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

/**
 * Hvilke perioder som må overlappe for at man skal kunne lage en vedtaksperiode
 */
enum class GrunnlagForVedtaksperiode {
    MÅLGRUPPE_OG_AKTIVITET,
    MÅLGRUPPE_OG_VILKÅR,
    MÅLGRUPPE_AKTIVITET_OG_VILKÅR,
}

val Stønadstype.grunnlagForVedtaksperiode: GrunnlagForVedtaksperiode
    get() =
        when (this) {
            Stønadstype.LÆREMIDLER -> GrunnlagForVedtaksperiode.MÅLGRUPPE_OG_AKTIVITET
            Stønadstype.FLYTTING_TSO,
            Stønadstype.FLYTTING_TSR,
            -> GrunnlagForVedtaksperiode.MÅLGRUPPE_OG_VILKÅR
            Stønadstype.BARNETILSYN,
            Stønadstype.BOUTGIFTER,
            Stønadstype.DAGLIG_REISE_TSO,
            Stønadstype.DAGLIG_REISE_TSR,
            Stønadstype.REISE_TIL_SAMLING_TSO,
            Stønadstype.REISE_TIL_SAMLING_TSR,
            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
            -> GrunnlagForVedtaksperiode.MÅLGRUPPE_AKTIVITET_OG_VILKÅR
        }

val Stønadstype.harAktivitet: Boolean
    get() = grunnlagForVedtaksperiode != GrunnlagForVedtaksperiode.MÅLGRUPPE_OG_VILKÅR
