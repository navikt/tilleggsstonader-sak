package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.tilVedtaksperiode
import org.springframework.stereotype.Service

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

        val vedtaksperioder =
            stønadsperiodeRepository
                .findAllByBehandlingId(behandling.id)
                .tilVedtaksperiode()
                .sorted()
                .splitFraRevurderFra(behandling.revurderFra)
        val perioder = tilsynBarnBeregningFellesService.beregnAktuellePerioder(behandling, typeVedtak, vedtaksperioder)
        val relevantePerioderFraForrigeVedtak =
            tilsynBarnBeregningFellesService.finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }
}
