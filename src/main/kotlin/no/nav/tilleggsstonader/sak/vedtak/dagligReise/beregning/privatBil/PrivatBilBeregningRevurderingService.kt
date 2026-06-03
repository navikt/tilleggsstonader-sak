package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.iDagHvisMandagEllerForrigeMandag
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PrivatBilBeregningRevurderingService(
    private val unleashService: UnleashService,
) {
    fun beregnRevurdering(
        forrigeRammevedtak: RammevedtakPrivatBil?,
        nyttRammevedtak: RammevedtakPrivatBil,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
        typeVedtak: TypeVedtak,
    ): RammevedtakPrivatBil? {
        // TODO: Håndter revurderinger som ikke er opphør
        brukerfeilHvis(typeVedtak == TypeVedtak.INNVILGELSE && forrigeRammevedtak != null) {
            "Vi støtter foreløpig bare revurderinger av daglige reiser med bil hvor resultatet er opphør."
        }

        if (typeVedtak == TypeVedtak.OPPHØR) {
            return kombinerRammevedtakForOpphør(
                forrigeRammevedtak = forrigeRammevedtak,
                nyttRammevedtak = nyttRammevedtak,
                avkortetVedtaksperioder = avkortetVedtaksperioder,
            )
        }

        return null
    }

    fun kombinerRammevedtakForOpphør(
        forrigeRammevedtak: RammevedtakPrivatBil?,
        nyttRammevedtak: RammevedtakPrivatBil,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
    ): RammevedtakPrivatBil? {
        brukerfeilHvis(!unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL)) {
            "Muligheten for å opphøre daglige reiser med privat bil er skrudd av."
        }

        if (forrigeRammevedtak == null) return nyttRammevedtak

        val beregnFraDato = finnBeregnFraDato(avkortetVedtaksperioder)

        val reiser =
            nyttRammevedtak.reiser.map { nyttReise ->
                val forrigeReise =
                    forrigeRammevedtak.reiser.find { it.reiseId == nyttReise.reiseId }

                slåSammenNyeOgGamlePerioderForReise(
                    forrigeRammevedtakForReise = forrigeReise,
                    nyttRammevedtakForReise = nyttReise,
                    beregnFra = beregnFraDato,
                )
            }

        return RammevedtakPrivatBil(reiser = reiser)
    }

    private fun slåSammenNyeOgGamlePerioderForReise(
        forrigeRammevedtakForReise: RammeForReiseMedPrivatBil?,
        nyttRammevedtakForReise: RammeForReiseMedPrivatBil,
        beregnFra: LocalDate,
    ): RammeForReiseMedPrivatBil {
        // Bruker nytt beregningsresultat dersom forrige ikke eksisterer
        feilHvis(forrigeRammevedtakForReise == null) {
            "Kun opphør støttes for privat bil, det bør derfor finnes et " +
                "rammevedtak for reisen fra forrige iverksatte behandling"
        }

        // Returnerer hele det gamle rammevedtak for reisen dersom hele reisen
        // er før beregn fra
        if (forrigeRammevedtakForReise.grunnlag.tom < beregnFra) {
            return forrigeRammevedtakForReise
        }

        val tidligereDelperioderSomSkalBeholdes =
            forrigeRammevedtakForReise.grunnlag.delperioder.filter { it.tom < beregnFra }
        val nyeDelperioder = nyttRammevedtakForReise.grunnlag.delperioder.filter { it.tom >= beregnFra }

        val alleDelperioder = (tidligereDelperioderSomSkalBeholdes + nyeDelperioder).sorted()

        brukerfeilHvis(alleDelperioder.isEmpty()) { "Det er foreløpig ikke mulig å opphøre en hel reise" }

        // Oppdaterer kun delperiodene fordi fom og tom på den nye beregningen av rammevedtaket vil være riktig
        return nyttRammevedtakForReise.copy(
            grunnlag =
                nyttRammevedtakForReise.grunnlag.copy(
                    delperioder = alleDelperioder,
                    fom = alleDelperioder.minOf { it.fom },
                    tom = alleDelperioder.maxOf { it.tom },
                ),
        )
    }

    /**
     * Midlertidig måte å finne beregn fra.
     * Denne bør hentes fra en beregningsplan, men foreløpig er beregningsplanen kun
     * tilpasset offentlig transport.
     */
    private fun finnBeregnFraDato(vedtaksperioder: List<Vedtaksperiode>): LocalDate {
        val opphørsdato = vedtaksperioder.maxOf { it.tom }.plusDays(1)

        return opphørsdato.iDagHvisMandagEllerForrigeMandag()
    }
}
