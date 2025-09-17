package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.finnPeriodeFraAndel as finnPeriodeTilsynBarnFraAndel
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.finnPeriodeFraAndel as finnPeriodeBoutgifterFraAndel
import no.nav.tilleggsstonader.sak.vedtak.læremidler.finnPerioderFraAndel as finnPerioderLæremidlerFraAndel

@RestController
@RequestMapping(
    path = ["/api/ekstern/tilbakekreving/andeler"],
)
@ProtectedWithClaims(issuer = "azuread")
class TilbakekrevingAndelerController(
    private val vedtakService: VedtakService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val behandlingService: BehandlingService,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
) {
    @GetMapping("/{eksternBehandlingId}")
    fun hentEndringIVedtakForTilbakekreving(
        @PathVariable("eksternBehandlingId") eksternBehandlingId: Long,
    ): AndelerUtbetalingDto {
        val behandlingId =
            eksternBehandlingIdRepository
                .findById(eksternBehandlingId)
                .orElseThrow { error("Finner ingen behandling med id $eksternBehandlingId") }
                .behandlingId

        val behandling = behandlingService.hentBehandling(behandlingId)

        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Behandling med id=$behandlingId har ingen forrige iverksatte behandling"
        }

        return AndelerUtbetalingDto(
            andelerForBehandling =
                mapAndelerForBehandling(behandling.id)
                    .sortedBy { it.utbetalingsperiode.fom },
            andelerForForrigeBehandling =
                mapAndelerForBehandling(behandling.forrigeIverksatteBehandlingId)
                    .sortedBy { it.utbetalingsperiode.fom },
        )
    }

    private fun mapAndelerForBehandling(behandlingId: BehandlingId): List<AndelUtbetalingMedVedtaksperiodeDto> {
        val andeler = tilkjentYtelseService.hentForBehandling(behandlingId).andelerTilkjentYtelse
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: error("Behandling $behandlingId har ingen vedtak")
        return andeler.map {
            AndelUtbetalingMedVedtaksperiodeDto(
                utbetalingsperiode =
                    PeriodeDto(
                        fom = it.fom,
                        tom = it.tom,
                    ),
                vedtaksperiode = if (it.erNullandel()) null else lagFaktiskPeriode(it, vedtak),
                beløp = it.beløp,
                typeAndel = it.type,
            )
        }
    }

    private fun lagFaktiskPeriode(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        vedtak: Vedtak,
    ): PeriodeDto {
        val vedtakdata = vedtak.data
        return when (vedtakdata) {
            is InnvilgelseEllerOpphørBoutgifter ->
                finnPeriodeBoutgifterFraAndel(vedtakdata.beregningsresultat, andelTilkjentYtelse)
                    .let { PeriodeDto(it.fom, it.tom) }
            is InnvilgelseEllerOpphørTilsynBarn ->
                finnPeriodeTilsynBarnFraAndel(vedtakdata.beregningsresultat, andelTilkjentYtelse)
                    .let { PeriodeDto(it.fom, it.tom) }
            is InnvilgelseEllerOpphørLæremidler ->
                finnPerioderLæremidlerFraAndel(vedtakdata.beregningsresultat, andelTilkjentYtelse)
                    .let { perioder -> PeriodeDto(perioder.minOf { it.fom }, perioder.maxOf { it.tom }) }
            is InnvilgelseEllerOpphørDagligReise -> TODO() // Vil gi mer mening å returnere flere perioder?
            else -> error("Behandling ${vedtak.behandlingId} har ikke et iverksatt vedtak")
        }
    }
}

data class AndelerUtbetalingDto(
    val andelerForBehandling: List<AndelUtbetalingMedVedtaksperiodeDto>,
    val andelerForForrigeBehandling: List<AndelUtbetalingMedVedtaksperiodeDto>,
)

data class AndelUtbetalingMedVedtaksperiodeDto(
    val utbetalingsperiode: PeriodeDto,
    val vedtaksperiode: PeriodeDto?,
    val beløp: Int,
    val typeAndel: TypeAndel,
)

data class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
)
