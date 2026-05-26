package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.iDagHvisMandagEllerForrigeMandag
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PrivatBilBeregningRevurderingService {
    /**
     * TODO: Tilpass denne for revurdering generelt og ikke bare opphør
     */
    fun kombinerRammevedtakForOpphør(
        forrigeRammevedtak: RammevedtakPrivatBil?,
        nyttRammevedtak: RammevedtakPrivatBil,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
        typeVedtak: TypeVedtak,
    ): RammevedtakPrivatBil? {
        brukerfeilHvis(typeVedtak != TypeVedtak.OPPHØR && forrigeRammevedtak != null) {
            "Vi støtter foreløpig bare revurderinger av daglige reiser med bil hvor resultatet er opphør."
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
                    beregnFra = beregnFraDato ?: LocalDate.now(),
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
        if (forrigeRammevedtakForReise == null) return nyttRammevedtakForReise

        // Returnerer hele det gamle rammevedtak for reisen dersom hele reisen
        // er før beregn fra
        if (forrigeRammevedtakForReise.grunnlag.tom < beregnFra) {
            return forrigeRammevedtakForReise
        }

        val tidligereDelperioderSomSkalBeholdes =
            forrigeRammevedtakForReise.grunnlag.delperioder.filter { it.tom < beregnFra }
        val nyeDelperioder = nyttRammevedtakForReise.grunnlag.delperioder.filter { it.fom >= beregnFra }

        val alleDelperioder = (tidligereDelperioderSomSkalBeholdes + nyeDelperioder).sorted()

        return nyttRammevedtakForReise.copy(
            grunnlag =
                nyttRammevedtakForReise.grunnlag.copy(
                    fom = alleDelperioder.minOf { it.fom },
                    tom = alleDelperioder.maxOf { it.tom },
                    delperioder = alleDelperioder,
                ),
        )
    }

    /**
     * Midlertidig måte å finne beregn fra.
     * Denne bør hentes fra en beregningsplan, men foreløpig er beregningsplanen kun
     * tilpasset offentlig transport.
     */
    private fun finnBeregnFraDato(vedtaksperioder: List<Vedtaksperiode>): LocalDate? {
        val opphørsdato = vedtaksperioder.maxOf { it.tom }.plusDays(1)

        return opphørsdato.iDagHvisMandagEllerForrigeMandag()
    }
}
