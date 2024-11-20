package no.nav.tilleggsstonader.sak.vedtak.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak

/**
 * Sealed class for å kunne ha egne objekter for hver type innvilgelse/opphør og avslag per stønadstype,
 * og kunne lagre ned alle disse i en og samme tabell, Vedtak
 */
sealed interface Vedtaksdata : VedtaksdataJson {
    val type: TypeVedtaksdata
}

@JsonDeserialize(using = TypeVedtaksdataDeserializer::class)
sealed interface TypeVedtaksdata {
    val typeVedtak: TypeVedtak
}
