package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

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
            val fraDato = finnBeregnFraDato(saksbehandling.stønadstype, tidligsteEndring, stønadsspesifikkJusteringAvBeregnFra)
            Beregningsplan(
                omfang = Beregningsomfang.FRA_DATO,
                fraDato = fraDato,
                tidligsteEndring = tidligsteEndring,
            )
        } else {
            Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT)
        }
    }

    companion object {
        fun utledForOpphørEllerSatsjustering(
            stønadstype: Stønadstype,
            opphørsdato: LocalDate,
            stønadsspesifikkJusteringAvBeregnFra: (LocalDate) -> LocalDate = { it },
        ): Beregningsplan =
            Beregningsplan(Beregningsomfang.FRA_DATO, finnBeregnFraDato(stønadstype, opphørsdato, stønadsspesifikkJusteringAvBeregnFra), tidligsteEndring = opphørsdato)

        private fun finnBeregnFraDato(
            stønadstype: Stønadstype,
            tidligsteEndring: LocalDate,
            stønadsspesifikkJusteringAvBeregnFra: (LocalDate) -> LocalDate = { it },
        ): LocalDate {
            val beregnFra =
                when (stønadstype) {
                    // For daglig reise vil vi reberegne perioder som starter opptil 29 dager unna tidligste endring-datoen. Det er fordi stønaden til
                    // offentlig transport er delt inn i 30-dagersperioder, og vi ellers ville kunne risikere at bruker får for mye penger. Se TS-Docs
                    // for eksempler.
                    Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR -> tidligsteEndring.minusDays(29)
                    else -> tidligsteEndring
                }
            return stønadsspesifikkJusteringAvBeregnFra(beregnFra)
        }
    }
}
