package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilsynBarnBeregningService {

    // Hva burde denne ta inn? Hva burde bli sendt inn i beregningscontroller?
    fun beregn(
        stønadsperioder: List<Stønadsperiode>,
        utgifter: Map<UUID, List<Utgift>>
    ): BeregningsresultatTilsynBarnDto {
        validerPerioder(stønadsperioder, utgifter)
        return BeregningsresultatTilsynBarnDto(emptyList())
    }

    private fun validerPerioder(
        stønadsperioder: List<Stønadsperiode>,
        utgifter: Map<UUID, List<Utgift>>
    ) {
        validerStønadsperioder(stønadsperioder)
        validerUtgifter(utgifter)
    }

    private fun validerStønadsperioder(stønadsperioder: List<Stønadsperiode>) {
        feilHvis(stønadsperioder.isEmpty()) {
            "Stønadsperioder mangler"
        }
        feilHvisIkke(stønadsperioder.erSortert()) {
            "Stønadsperioder er ikke sortert"
        }
        feilHvis(stønadsperioder.overlapper()) {
            "Stønadsperioder overlapper"
        }
    }

    private fun validerUtgifter(utgifter: Map<UUID, List<Utgift>>) {
        feilHvis(utgifter.values.flatten().isEmpty()) {
            "Utgiftsperioder mangler"
        }
        utgifter.entries.forEach { (_, utgifterForBarn) ->
            feilHvisIkke(utgifterForBarn.erSortert()) {
                "Utgiftsperioder er ikke sortert"
            }
            feilHvis(utgifterForBarn.overlapper()) {
                "Utgiftsperioder overlapper"
            }

        }
    }
}
