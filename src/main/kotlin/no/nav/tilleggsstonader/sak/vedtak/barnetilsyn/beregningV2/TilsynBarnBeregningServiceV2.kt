package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningFellesService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilVedtaksperiodeBeregingsgrunnlag
import org.springframework.stereotype.Service

@Service
class TilsynBarnBeregningServiceV2(
    private val tilsynBarnBeregningFellesService: TilsynBarnBeregningFellesService,
) {
    fun beregn(
        vedtaksperioder: List<VedtaksperiodeDto>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        feilHvis(typeVedtak == TypeVedtak.AVSLAG) {
            "Skal ikke beregne for avslag"
        }
        // TODO Valider ingen overlapp mellom vedtaksperioder?

        val vedtaksperioderEtterRevurderFra =
            vedtaksperioder
                .tilVedtaksperiodeBeregingsgrunnlag()
                .sorted()
                .splitFraRevurderFra(behandling.revurderFra)
        val perioder =
            tilsynBarnBeregningFellesService.beregnAktuellePerioder(
                behandling,
                typeVedtak,
                vedtaksperioderEtterRevurderFra,
            )
        val relevantePerioderFraForrigeVedtak =
            tilsynBarnBeregningFellesService.finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }
}
