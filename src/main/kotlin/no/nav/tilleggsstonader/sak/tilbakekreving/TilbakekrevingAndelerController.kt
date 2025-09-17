package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.AndelTilkjentYtelseTilPeriodeService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
    private val andelTilkjentYtelseTilPeriodeService: AndelTilkjentYtelseTilPeriodeService,
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

    private fun mapAndelerForBehandling(behandlingId: BehandlingId): List<AndelUtbetalingMedVedtaksperiodeDto> =
        andelTilkjentYtelseTilPeriodeService
            .mapAndelerTilVedtaksperiodeForBehandling(behandlingId)
            .map {
                AndelUtbetalingMedVedtaksperiodeDto(
                    utbetalingsperiode =
                        PeriodeDto(
                            fom = it.andelTilkjentYtelse.fom,
                            tom = it.andelTilkjentYtelse.tom,
                        ),
                    vedtaksperiode = it.vedtaksperiode?.let { v -> PeriodeDto(v.fom, v.tom) },
                    beløp = it.andelTilkjentYtelse.beløp,
                    typeAndel = it.andelTilkjentYtelse.type,
                )
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
