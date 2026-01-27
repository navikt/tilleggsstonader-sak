package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseVedtakService(
    private val vedtakRepository: VedtakRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val simuleringService: SimuleringService,
) {
    fun lagreInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatDagligReise,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        tidligsteEndring: LocalDate?,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseDagligReise(
                        vedtaksperioder = vedtaksperioder,
                        begrunnelse = begrunnelse,
                        beregningsresultat = beregningsresultat,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = tidligsteEndring,
            ),
        )
    }

    fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagDagligReiseDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagDagligReise(
                        årsaker = vedtak.årsakerAvslag,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            ),
        )
    }

    fun lagreOpphørsvedtak(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatDagligReise,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
        vedtak: OpphørDagligReiseRequest,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørDagligReise(
                        beregningsresultat = beregningsresultat,
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                        vedtaksperioder = avkortetVedtaksperioder,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = vedtak.opphørsdato,
            ),
        )
    }

    fun hentVedtak(behandlingId: BehandlingId): GeneriskVedtak<InnvilgelseEllerOpphørDagligReise> =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()

    fun nullstillEksisterendeVedtakPåBehandling(saksbehandling: Saksbehandling) {
        vedtakRepository.deleteById(saksbehandling.id)
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
    }

    fun avkortVedtaksperiodeVedOpphør(
        forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørDagligReise>,
        opphørsdato: LocalDate,
    ): List<Vedtaksperiode> = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(opphørsdato.minusDays(1))
}
