package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelsePassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilLagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.AvslagPassAvBarnDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.InnvilgelsePassAvBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.OpphørPassAvBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.VedtakPassAvBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.InnvilgelseReiseOppstartAvslutningHjemreiseResponse
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.VedtakReiseOppstartAvslutningHjemreiseResponse
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.AvslagReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingResponse
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.VedtakReiseTilSamlingResponse
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class VedtakDtoMapper(
    private val vedtakService: VedtakService,
    private val dagligReiseVilkårService: DagligReiseVilkårService,
) {
    fun toDto(
        vedtak: Vedtak,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakResponse =
        when (val data = vedtak.data) {
            is VedtakPassAvBarn ->
                mapVedtakPassAvBarn(
                    vedtak,
                    data,
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
                    vedtak = vedtak,
                    data = data,
                    vilkår = dagligReiseVilkårService.hentVilkårForBehandling(vedtak.behandlingId),
                    tidligsteEndring = vedtak.tidligsteEndring,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
            is VedtakReiseTilSamling ->
                mapVedtakReiseTilSamling(
                    vedtak = vedtak,
                    data = data,
                    tidligsteEndring = vedtak.tidligsteEndring,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
            is VedtakReiseOppstartAvslutningHjemreise ->
                mapVedtakReiseOppstartAvslutningHjemreise(
                    vedtak = vedtak,
                    data = data,
                    tidligsteEndring = vedtak.tidligsteEndring,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
        }

    private fun mapVedtakPassAvBarn(
        vedtak: Vedtak,
        data: VedtakPassAvBarn,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakPassAvBarnResponse =
        when (data) {
            is InnvilgelsePassAvBarn ->
                InnvilgelsePassAvBarnResponse(
                    beregningsresultat = data.beregningsresultat.tilDto(beregningsplan = data.beregningsplan),
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    begrunnelse = data.begrunnelse,
                )

            is OpphørPassAvBarn ->
                OpphørPassAvBarnResponse(
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder = data.vedtaksperioder.tilLagretVedtaksperiodeDto(null),
                    opphørsdato = vedtak.opphørsdato ?: feil("Opphørsdato er obligatorisk for opphør"),
                )

            is AvslagPassAvBarn ->
                AvslagPassAvBarnDto(
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
            is InnvilgelseLæremidler ->
                InnvilgelseLæremidlerResponse(
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    beregningsresultat = data.beregningsresultat.tilDto(beregningsplan = data.beregningsplan),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).minOfOrNull { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).maxOfOrNull { it.tom },
                    begrunnelse = data.begrunnelse,
                )

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
                    opphørsdato = vedtak.opphørsdato ?: feil("Opphørsdato er obligatorisk for opphør"),
                    beregningsplan = data.beregningsplan.tilDto(),
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
                    beregningsresultat = data.beregningsresultat.tilDto(beregningsplan = data.beregningsplan),
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
                    opphørsdato = vedtak.opphørsdato ?: feil("Opphørsdato er obligatorisk for opphør"),
                    beregningsplan = data.beregningsplan.tilDto(),
                )
        }

    private fun mapVedtakDagligReise(
        vedtak: Vedtak,
        data: VedtakDagligReise,
        vilkår: List<VilkårDagligReise>,
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
                    beregningsresultat = data.beregningsresultat.tilDto(beregningsplan = data.beregningsplan, vilkår),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).minOfOrNull { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).maxOfOrNull { it.tom },
                    begrunnelse = data.begrunnelse,
                    rammevedtakPrivatBil = data.rammevedtakPrivatBil?.tilDto(data.beregningsplan),
                )
            }

            is AvslagDagligReise -> AvslagDagligReiseDto(årsakerAvslag = data.årsaker, begrunnelse = data.begrunnelse)
            is OpphørDagligReise ->
                OpphørDagligReiseResponse(
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    opphørsdato = vedtak.opphørsdato ?: feil("Opphørsdato er obligatorisk for opphør"),
                )
        }

    private fun mapVedtakReiseTilSamling(
        vedtak: Vedtak,
        data: VedtakReiseTilSamling,
        tidligsteEndring: LocalDate?,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakReiseTilSamlingResponse =
        when (data) {
            is InnvilgelseReiseTilSamling -> {
                InnvilgelseReiseTilSamlingResponse(
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    beregningsresultat = data.beregningsresultat.tilDto(beregningsplan = data.beregningsplan),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).minOfOrNull { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).maxOfOrNull { it.tom },
                    begrunnelse = data.begrunnelse,
                )
            }
            is AvslagReiseTilSamling ->
                AvslagReiseTilSamlingDto(
                    årsakerAvslag = data.årsaker,
                    begrunnelse = data.begrunnelse,
                )
        }

    private fun mapVedtakReiseOppstartAvslutningHjemreise(
        vedtak: Vedtak,
        data: VedtakReiseOppstartAvslutningHjemreise,
        tidligsteEndring: LocalDate?,
        forrigeIverksatteBehandlingId: BehandlingId?,
    ): VedtakReiseOppstartAvslutningHjemreiseResponse =
        when (data) {
            is InnvilgelseReiseOppstartAvslutningHjemreise -> {
                InnvilgelseReiseOppstartAvslutningHjemreiseResponse(
                    vedtaksperioder =
                        data.vedtaksperioder.tilLagretVedtaksperiodeDto(
                            hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId),
                        ),
                    beregningsresultat = data.beregningsresultat.tilDto(beregningsplan = data.beregningsplan),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).minOfOrNull { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(tidligsteEndring).maxOfOrNull { it.tom },
                    begrunnelse = data.begrunnelse,
                )
            }
        }

    private fun hentForrigeVedtaksperioder(forrigeIverksatteBehandlingId: BehandlingId?): List<Vedtaksperiode>? =
        forrigeIverksatteBehandlingId?.let {
            vedtakService.hentVedtaksperioder(behandlingId = it)
        }
}
