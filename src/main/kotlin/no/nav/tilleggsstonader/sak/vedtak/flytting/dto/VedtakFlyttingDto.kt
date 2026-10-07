package no.nav.tilleggsstonader.sak.vedtak.flytting.dto

import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeTsrDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlytting
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatFlyttingDto(
    val resultater: List<BeregningsresultatFlyttevilkårDto>,
)

data class BeregningsresultatFlyttevilkårDto(
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
) : VedtakFlyttingResponse

sealed interface VedtakFlyttingResponse : VedtakResponse

sealed interface InnvilgelseFlyttingRequest : VedtakRequest {
    val begrunnelse: String?

    fun vedtaksperioder(): List<Vedtaksperiode>
}

data class InnvilgelseFlyttingTsoRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseFlyttingRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}

data class InnvilgelseFlyttingTsrRequest(
    val vedtaksperioder: List<VedtaksperiodeTsrDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseFlyttingRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}

fun BeregningsresultatFlytting.tilDto() =
    BeregningsresultatFlyttingDto(
        resultater =
            resultater.map {
                BeregningsresultatFlyttevilkårDto(
                    fom = it.fom,
                    tom = it.tom,
                    grunnlag = it.grunnlag,
                    beløp = it.beløp,
                )
            },
    )
