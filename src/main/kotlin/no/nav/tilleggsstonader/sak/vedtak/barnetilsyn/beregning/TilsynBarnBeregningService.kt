package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtilsFelles.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregingsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Stønaden dekker 64% av utgifterne til barnetilsyn
 */
val DEKNINGSGRAD_TILSYN_BARN = BigDecimal("0.64")

@Service
class TilsynBarnBeregningService(
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val tilsynBarnBeregningFellesService: TilsynBarnBeregningFellesService,
) {
    fun beregn(
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        feilHvis(typeVedtak == TypeVedtak.AVSLAG) {
            "Skal ikke beregne for avslag"
        }
        val stønadsperioder =
            stønadsperiodeRepository
                .findAllByBehandlingId(behandling.id)
                .tilVedtaksperiodeBeregingsgrunnlag()
                .sorted()
                .splitFraRevurderFra(behandling.revurderFra)
        val perioder = tilsynBarnBeregningFellesService.beregnAktuellePerioder(behandling, typeVedtak, stønadsperioder)
        val relevantePerioderFraForrigeVedtak =
            tilsynBarnBeregningFellesService.finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }
}
