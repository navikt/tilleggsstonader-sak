package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import java.time.LocalDate

/**
 * Beregningsresultatet kan være en del av en trettidagers periode - Eks 1jan-30jan
 */
data class InnvilgelseDagligReiseResponse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val beregningsresultat: BeregningsresultatDagligReiseDto,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
    val begrunnelse: String? = null,
) : VedtakDagligReiseDto(TypeVedtak.INNVILGELSE),
    VedtakDagligReiseResponse

sealed interface InnvilgelseDagligReiseRequest : VedtakDagligReiseRequest {
    val begrunnelse: String?

    fun vedtaksperioder(): List<Vedtaksperiode>
}

@Deprecated("Går over til å bruke egne typer for tso og tsr da vedaksperiodene er forskjellig bygget opp")
data class InnvilgelseDagligReiseRequestGammel(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseDagligReiseRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}

data class InnvilgelseDagligReiseTsoRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseDagligReiseRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}

data class InnvilgelseDagligReiseTsrRequest(
    val vedtaksperioder: List<VedtaksperiodeDagligReiseTsrDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseDagligReiseRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}
