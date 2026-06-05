package no.nav.tilleggsstonader.sak.vedtak

import java.time.LocalDate
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service

@Service
class BeregningsplanUtleder(
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun utledForInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsspesifikkJusteringAvBeregnFra: (LocalDate) -> LocalDate = { it },
    ): Beregningsplan {
        if (saksbehandling.forrigeIverksatteBehandlingId == null) {
            return Beregningsplan(Beregningsomfang.ALLE_PERIODER)
        }
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(saksbehandling.id, vedtaksperioder)
        return if (tidligsteEndring != null) {
            Beregningsplan(
                omfang = Beregningsomfang.FRA_DATO,
                fraDato = stønadsspesifikkJusteringAvBeregnFra(tidligsteEndring),
                tidligsteEndring = tidligsteEndring,
            )
        } else {
            Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT)
        }
    }

    companion object {
        fun utledForOpphørEllerSatsjustering(
            opphørsdato: LocalDate,
            stønadsspesifikkJusteringAvBeregnFra: (LocalDate) -> LocalDate = { it },
        ): Beregningsplan = Beregningsplan(
            omfang = Beregningsomfang.FRA_DATO,
            fraDato = stønadsspesifikkJusteringAvBeregnFra(opphørsdato),
            tidligsteEndring = opphørsdato
        )
    }
}
