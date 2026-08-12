package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
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
        beregningsplan: Beregningsplan,
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
                    beregningsplan = beregningsplan,
                )
            }

        if (reiser.isEmpty()) return null

        return RammevedtakPrivatBil(
            reiser = reiser,
        )
    }

    private fun beregnRammevedtakForReiseIRevurdering(
        vilkårStatus: VilkårStatus?,
        forrigeRammeForReise: RammevedtakForReiseMedPrivatBil?,
        nyRammeForReise: RammevedtakForReiseMedPrivatBil?,
        beregningsplan: Beregningsplan,
    ): RammevedtakForReiseMedPrivatBil? =
        when (vilkårStatus) {
            VilkårStatus.NY -> nyRammeForReise ?: feil("Forventer at det finnes et nytt rammevedtak for nye reiser")
            VilkårStatus.SLETTET -> null
            VilkårStatus.ENDRET, VilkårStatus.UENDRET ->
                velgRammeForReiseBasertPåBeregningsplan(
                    forrigeRammeForReise,
                    nyRammeForReise,
                    beregningsplan,
                    vilkårStatus,
                )

            null -> feil("Forventer at alle vilkår har en status.")
        }

    private fun velgRammeForReiseBasertPåBeregningsplan(
        forrigeRammeForReise: RammevedtakForReiseMedPrivatBil?,
        nyRammeForReise: RammevedtakForReiseMedPrivatBil?,
        beregningsplan: Beregningsplan,
        vilkårStatus: VilkårStatus,
    ): RammevedtakForReiseMedPrivatBil? =
        when (beregningsplan.omfang) {
            Beregningsomfang.ALLE_PERIODER -> nyRammeForReise
            Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT -> forrigeRammeForReise
            Beregningsomfang.KUN_NYE_KJORELISTE_UKER -> forrigeRammeForReise
            Beregningsomfang.FRA_DATO ->
                velgRammeForReiseBasertPåBeregnFraDato(
                    beregnFra = beregningsplan.fraDato ?: feil("Forventer at fraDato finnes for beregningsplan med omfang FRA_DATO"),
                    forrigeRammeForReise = forrigeRammeForReise,
                    nyRammeForReise = nyRammeForReise,
                    vilkårStatus = vilkårStatus,
                )
        }

    private fun velgRammeForReiseBasertPåBeregnFraDato(
        beregnFra: LocalDate,
        forrigeRammeForReise: RammevedtakForReiseMedPrivatBil?,
        nyRammeForReise: RammevedtakForReiseMedPrivatBil?,
        vilkårStatus: VilkårStatus,
    ): RammevedtakForReiseMedPrivatBil {
        feilHvis(forrigeRammeForReise == null || nyRammeForReise == null) {
            "Forventer at det finnes et rammevedtak for reise både i eksisterende og ny beregning når vilkår har status $vilkårStatus"
        }

        val reiseErFørberegnFra =
            forrigeRammeForReise.grunnlag.tom < beregnFra &&
                nyRammeForReise.grunnlag.tom < beregnFra

        return if (reiseErFørberegnFra) {
            forrigeRammeForReise
        } else {
            validerReisedagerIkkeRedusert(forrigeRammeForReise, nyRammeForReise)
            nyRammeForReise
        }
    }

    /**
     * Vi støtter ikke å redusere antall reisedager per uke i en revurdering, da reduksjon kan komme i konflikt med
     * dager som allerede er kjørt og utbetalt. Økning av reisedager er tillatt.
     */
    private fun validerReisedagerIkkeRedusert(
        forrigeRammeForReise: RammevedtakForReiseMedPrivatBil,
        nyRammeForReise: RammevedtakForReiseMedPrivatBil,
    ) {
        if (!unleashService.isEnabled(Toggle.KAN_REDUSERE_REISEDAGER_REVURDERING_PRIVAT_BIL)) {
            val reisedagerErRedusert =
                nyRammeForReise.grunnlag.delperioder.any { nyDelperiode ->
                    forrigeRammeForReise.grunnlag.delperioder
                        .filter { it.overlapper(nyDelperiode) }
                        .any { nyDelperiode.reisedagerPerUke < it.reisedagerPerUke }
                }

            brukerfeilHvis(reisedagerErRedusert) {
                "Det er ikke støttet å redusere antall reisedager per uke i en revurdering, da dette kan komme i konflikt med dager som allerede er kjørt og utbetalt."
            }
        }
    }
}
