package no.nav.tilleggsstonader.sak.vedtak.flytting.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlytting
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatFlyttingDto(
    val resultater: List<BeregningsresultatFlyttevilkårDto>,
)

data class BeregningsresultatFlyttevilkårDto(
    val fraTidligereVedtak: Boolean,
    val fom: LocalDate,
    val tom: LocalDate,
    val grunnlag: BeregningsgrunnlagFlytting,
    val beløp: BigDecimal,
)

data class InnvilgelseFlyttingResponse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>,
    val beregningsresultat: BeregningsresultatFlyttingDto,
    val begrunnelse: String?,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
) : VedtakFlyttingDto(TypeVedtak.INNVILGELSE),
    VedtakFlyttingResponse

data class AvslagFlyttingDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakFlyttingDto(TypeVedtak.AVSLAG),
    VedtakFlyttingRequest,
    VedtakFlyttingResponse

data class OpphørFlyttingResponse(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val opphørsdato: LocalDate,
) : VedtakFlyttingDto(TypeVedtak.OPPHØR),
    VedtakFlyttingResponse

sealed class VedtakFlyttingDto(
    open val type: TypeVedtak,
)

sealed interface VedtakFlyttingResponse : VedtakResponse

sealed interface VedtakFlyttingRequest : VedtakRequest

data class OpphørFlyttingRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakFlyttingDto(TypeVedtak.OPPHØR),
    VedtakFlyttingRequest

sealed interface InnvilgelseFlyttingRequest : VedtakFlyttingRequest {
    val begrunnelse: String?

    fun vedtaksperioder(): List<Vedtaksperiode>
}

data class InnvilgelseFlyttingTsoRequest(
    val vedtaksperioder: List<VedtaksperiodeFlyttingDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseFlyttingRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}

data class InnvilgelseFlyttingTsrRequest(
    val vedtaksperioder: List<VedtaksperiodeFlyttingDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseFlyttingRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}

fun BeregningsresultatFlytting.tilDto() =
    BeregningsresultatFlyttingDto(
        resultater =
            resultater.map {
                BeregningsresultatFlyttevilkårDto(
                    fraTidligereVedtak = it.fraTidligereVedtak,
                    fom = it.fom,
                    tom = it.tom,
                    grunnlag = it.grunnlag,
                    beløp = it.beløp,
                )
            },
    )
