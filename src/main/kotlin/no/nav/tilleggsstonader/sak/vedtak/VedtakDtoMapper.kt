package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.tilLagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDto
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class VedtakDtoMapper(
    private val vedtakService: VedtakService,
) {
    fun toDto(
        vedtak: Vedtak,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakResponse {
        val data = vedtak.data
        return when (data) {
            is VedtakTilsynBarn ->
                mapVedtakTilsynBarn(
                    vedtak,
                    data,
                    vedtak.tidligsteEndring,
                    forrigeIverksatteBehandlingId,
                )

            is VedtakLæremidler ->
                mapVedtakLæremidler(
                    vedtak,
                    data,
                    vedtak.tidligsteEndring,
                    forrigeIverksatteBehandlingId,
                )

            is VedtakBoutgifter ->
                mapVedtakBoutgifter(
                    vedtak,
                    data,
                    vedtak.tidligsteEndring,
                    forrigeIverksatteBehandlingId,
                )

            is VedtakDagligReise ->
                mapVedtakDagligReise(
                    vedtak,
                    data,
                    vedtak.tidligsteEndring,
                    forrigeIverksatteBehandlingId,
                )
        }
    }

    private fun mapVedtakTilsynBarn(
        vedtak: Vedtak,
        data: VedtakTilsynBarn,
        tidligsteEndring: LocalDate?,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakTilsynBarnResponse =
        when (data) {
            is InnvilgelseTilsynBarn -> {
                InnvilgelseTilsynBarnResponse(
                    beregningsresultat = data.beregningsresultat.tilDto(tidligsteEndring = tidligsteEndring),
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    begrunnelse = data.begrunnelse,
                )
            }

            is OpphørTilsynBarn ->
                OpphørTilsynBarnResponse(
                    beregningsresultat = data.beregningsresultat.tilDto(tidligsteEndring = tidligsteEndring),
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder = data.vedtaksperioder.tilLagretVedtaksperiodeDto(null),
                    opphørsdato = vedtak.opphørsdato,
                )

            is AvslagTilsynBarn ->
                AvslagTilsynBarnDto(
                    årsakerAvslag = data.årsaker,
                    begrunnelse = data.begrunnelse,
                )
        }

    private fun mapVedtakLæremidler(
        vedtak: Vedtak,
        data: VedtakLæremidler,
        tidligsteEndring: LocalDate?,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakLæremidlerResponse =
        when (data) {
            is InnvilgelseLæremidler -> {
                InnvilgelseLæremidlerResponse(
                    vedtaksperioder =
                        data.vedtaksperioder
                            .tilLagretVedtaksperiodeDto(hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId)),
                    beregningsresultat = data.beregningsresultat.tilDto(tidligsteEndring = tidligsteEndring),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).minOfOrNull { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).maxOfOrNull { it.tom },
                    begrunnelse = data.begrunnelse,
                )
            }

            is AvslagLæremidler ->
                AvslagLæremidlerDto(
                    årsakerAvslag = data.årsaker,
                    begrunnelse = data.begrunnelse,
                )

            is OpphørLæremidler ->
                OpphørLæremidlerResponse(
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder =
                        data.vedtaksperioder
                            .tilLagretVedtaksperiodeDto(hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId)),
                    opphørsdato = vedtak.opphørsdato,
                )
        }

    private fun mapVedtakBoutgifter(
        vedtak: Vedtak,
        data: VedtakBoutgifter,
        tidligsteEndring: LocalDate?,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakBoutgifterResponse =
        when (data) {
            is InnvilgelseBoutgifter -> {
                InnvilgelseBoutgifterResponse(
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    beregningsresultat = data.beregningsresultat.tilDto(tidligsteEndring = tidligsteEndring),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).minOfOrNull { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).maxOfOrNull { it.tom },
                    begrunnelse = data.begrunnelse,
                )
            }

            is AvslagBoutgifter ->
                AvslagBoutgifterDto(
                    årsakerAvslag = data.årsaker,
                    begrunnelse = data.begrunnelse,
                )

            is OpphørBoutgifter ->
                OpphørBoutgifterResponse(
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    opphørsdato = vedtak.opphørsdato,
                )
        }

    private fun mapVedtakDagligReise(
        vedtak: Vedtak,
        data: VedtakDagligReise,
        tidligsteEndring: LocalDate?,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakDagligReiseResponse =
        when (data) {
            is InnvilgelseDagligReise -> {
                InnvilgelseDagligReiseResponse(
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    beregningsresultat = data.beregningsresultat,
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).minOfOrNull { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).maxOfOrNull { it.tom },
                    begrunnelse = data.begrunnelse,
                )
            }

            is AvslagDagligReise -> TODO()
            is OpphørDagligReise -> TODO()
        }

    private fun hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId: BehandlingId?): List<Vedtaksperiode>? =
        forrigeIverksatteBehandlingId?.let {
            vedtakService.hentVedtaksperioder(behandlingId = it)
        }
}
