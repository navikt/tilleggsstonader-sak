package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class PrivatBilBeregningRevurderingService(
    private val unleashService: UnleashService,
) {
    fun kuttEksisterendeRammevedtakForOpphør(
        forrigeRammevedtak: RammevedtakPrivatBil,
        opphørsdato: LocalDate?,
    ): RammevedtakPrivatBil? {
        brukerfeilHvis(!unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL)) {
            "Muligheten for å opphøre daglige reiser med privat bil er skrudd av."
        }

        feilHvis(opphørsdato == null) {
            "Opphørsdato må være satt for å kunne opphøre"
        }

        brukerfeilHvis(opphørsdato.dayOfWeek != DayOfWeek.MONDAY) {
            "Foreløpig støtter vi kun opphør av hele uker for daglige reiser med privat bil. Sett opphørsdato til en mandag"
        }

        val avkortedeReiser = forrigeRammevedtak.reiser.mapNotNull { it.avkortEtterDato(opphørsdato.minusDays(1)) }

        if (avkortedeReiser.isEmpty()) return null

        return RammevedtakPrivatBil(
            reiser = avkortedeReiser,
        )
    }
}
