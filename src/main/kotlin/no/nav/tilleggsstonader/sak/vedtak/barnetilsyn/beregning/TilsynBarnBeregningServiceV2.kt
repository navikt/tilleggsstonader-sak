package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilVedtaksperiode
import org.springframework.stereotype.Service

@Service
class TilsynBarnBeregningServiceV2(
    private val tilsynBarnBeregningFellesService: TilsynBarnBeregningFellesService,
) {
    fun beregn(
        vedtaksperioderDto: List<VedtaksperiodeDto>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        feilHvis(typeVedtak == TypeVedtak.AVSLAG) {
            "Skal ikke beregne for avslag"
        }
        // TODO Valider ingen overlapp mellom vedtaksperioderDto

        val vedtaksperioder =
            vedtaksperioderDto.tilVedtaksperiode().sorted().splitFraRevurderFra(behandling.revurderFra)

        val perioder = tilsynBarnBeregningFellesService.beregnAktuellePerioder(behandling, typeVedtak, vedtaksperioder)
        val relevantePerioderFraForrigeVedtak =
            tilsynBarnBeregningFellesService.finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }
}
