package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import org.springframework.stereotype.Service
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

        val avkortedeReiser = forrigeRammevedtak.reiser.mapNotNull { it.avkortEtterDato(opphørsdato.minusDays(1)) }

        if (avkortedeReiser.isEmpty()) return null

        return RammevedtakPrivatBil(
            reiser = avkortedeReiser,
        )
    }

    fun beregnRammevedtakVedRevurdering(
        reiserMedBil: List<ReiseMedPrivatBil>,
        forrigeRammevedtak: RammevedtakPrivatBil,
        nyttRammevedtak: RammevedtakPrivatBil?,
        tidligsteEndring: LocalDate?,
    ): RammevedtakPrivatBil? {
        brukerfeilHvis(!unleashService.isEnabled(Toggle.KAN_REVURDERE_PRIVAT_BIL)) {
            "Muligheten for å revurdere daglige reiser med privat bil er skrudd av."
        }

        val forrigeRammerByReiseId = forrigeRammevedtak.reiser.associateBy { it.reiseId }
        val nyeRammerByReiseId = nyttRammevedtak?.reiser?.associateBy { it.reiseId }

        val reiser =
            reiserMedBil.mapNotNull { reise ->
                beregnRammevedtakForReiseIRevurdering(
                    vilkårStatus = reise.status,
                    forrigeRammeForReise = forrigeRammerByReiseId[reise.reiseId],
                    nyRammeForReise = nyeRammerByReiseId?.get(reise.reiseId),
                    tidligsteEndring = tidligsteEndring,
                )
            }

        if (reiser.isEmpty()) return null

        return RammevedtakPrivatBil(
            reiser = reiser,
        )
    }

    private fun beregnRammevedtakForReiseIRevurdering(
        vilkårStatus: VilkårStatus?,
        forrigeRammeForReise: RammeForReiseMedPrivatBil?,
        nyRammeForReise: RammeForReiseMedPrivatBil?,
        tidligsteEndring: LocalDate?,
    ): RammeForReiseMedPrivatBil? =
        when (vilkårStatus) {
            VilkårStatus.NY -> nyRammeForReise ?: feil("Forventer at det finnes et nytt rammevedtak for nye reiser")
            VilkårStatus.SLETTET -> null
            VilkårStatus.ENDRET, VilkårStatus.UENDRET ->
                velgRammeForReiseBasertPåTidligsteEndring(
                    forrigeRammeForReise,
                    nyRammeForReise,
                    tidligsteEndring,
                    vilkårStatus,
                )

            null -> feil("Forventer at alle vilkår har en status.")
        }

    private fun velgRammeForReiseBasertPåTidligsteEndring(
        forrigeRammeForReise: RammeForReiseMedPrivatBil?,
        nyRammeForReise: RammeForReiseMedPrivatBil?,
        tidligsteEndring: LocalDate?,
        vilkårStatus: VilkårStatus,
    ): RammeForReiseMedPrivatBil? {
        feilHvis(tidligsteEndring == null) {
            "Forventer at tidligste endring finnes for en revurdering"
        }

        feilHvis(forrigeRammeForReise == null || nyRammeForReise == null) {
            "Forventer at det finnes et rammevedtak for reise både i eksisterende og ny beregning når vilkår har status $vilkårStatus"
        }

        val reiseErFørTidligsteEndring =
            forrigeRammeForReise.grunnlag.tom < tidligsteEndring &&
                nyRammeForReise.grunnlag.tom < tidligsteEndring

        return if (reiseErFørTidligsteEndring) {
            forrigeRammeForReise
        } else {
            nyRammeForReise
        }
    }
}
