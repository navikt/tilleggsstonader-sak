package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingMapper.mapTilVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import org.springframework.stereotype.Service

@Service
class ReiseTilSamlingBeregningService(
    private val vilkårService: VilkårService,
    private val vedtakService: VedtakService,
) {
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        typeVedtak: TypeVedtak,
    ): BeregningReiseTilSamling {
        val oppfylteVilkårReiseTilSamling =
            vilkårService
                .hentOppfylteReiseTilSamlingVilkår(
                    behandling.id,
                ).map { it.mapTilVilkårReiseTilSamling() }

        validerFinnesSamling(oppfylteVilkårReiseTilSamling)

        val oppfylteOffentligTransport = oppfylteVilkårReiseTilSamling.filter { it.fakta is FaktaOffentligTransport }
        val offentligTransport =
            BeregningsresultatOffentligTransport(
                samling =
                    oppfylteOffentligTransport.map { samling ->
                        samling.fakta as FaktaOffentligTransport
                        BeregningsresultatForSamling(
                            reiseId = samling.fakta.reiseId,
                            adresse = samling.fakta.adresse,
                            fom = samling.fom,
                            tom = samling.tom,
                            utgifterOffentligTransport = samling.fakta.utgifterOffentligTransport,
                        )
                    },
                beløp = oppfylteOffentligTransport.sumOf { (it.fakta as FaktaOffentligTransport).utgifterOffentligTransport ?: 0 },
            )

        return BeregningReiseTilSamling(
            beregningsresultatOffentligTransport = offentligTransport,
        )
    }
}

private fun validerFinnesSamling(vilkår: List<VilkårReiseTilSamling>) {
    brukerfeilHvis(vilkår.isEmpty()) {
        "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med samling"
    }
}

data class BeregningReiseTilSamling(
    val beregningsresultatOffentligTransport: BeregningsresultatOffentligTransport,
)
