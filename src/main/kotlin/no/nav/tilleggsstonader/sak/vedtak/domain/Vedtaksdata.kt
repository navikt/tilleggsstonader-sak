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

sealed interface Innvilgelse : Vedtaksdata

sealed interface Opphør : Vedtaksdata {
    val årsaker: List<ÅrsakOpphør>
    val begrunnelse: String
}

sealed interface Avslag : Vedtaksdata {
    val årsaker: List<ÅrsakAvslag>
    val begrunnelse: String
}

fun Avslag.validerÅrsakerOgBegrunnelse() {
    require(årsaker.isNotEmpty()) { "Må velge minst en årsak for avslag" }
    require(begrunnelse.isNotBlank()) { "Avslag må begrunnes" }
}

fun Opphør.validerÅrsakerOgBegrunnelse() {
    require(årsaker.isNotEmpty()) { "Må velge minst en årsak for opphør" }
    require(begrunnelse.isNotBlank()) { "Opphør må begrunnes" }
}
