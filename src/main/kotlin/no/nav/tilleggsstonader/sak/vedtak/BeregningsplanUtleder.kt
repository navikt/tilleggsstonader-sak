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
    ): Beregningsplan {
        if (saksbehandling.forrigeIverksatteBehandlingId == null) {
            return Beregningsplan(Beregningsomfang.ALLE_PERIODER)
        }
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(saksbehandling.id, vedtaksperioder)
        return if (tidligsteEndring != null) {
            Beregningsplan(
                omfang = Beregningsomfang.FRA_DATO,
                fraDato = finnBeregnFraDato(saksbehandling.stønadstype, tidligsteEndring),
            )
        } else {
            Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT)
        }
    }

    companion object {
        fun utledForOpphørEllerSatsjustering(
            stønadstype: Stønadstype,
            opphørsdato: LocalDate,
        ): Beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, finnBeregnFraDato(stønadstype, opphørsdato))

        private fun finnBeregnFraDato(
            stønadstype: Stønadstype,
            tidligsteEndring: LocalDate,
        ): LocalDate =
            when (stønadstype) {
                // For daglig reise vil vi reberegne perioder som starter opptil 29 dager unna tidligste endring-datoen. Det er fordi stønaden til
                // offentlig transport er delt inn i 30-dagersperioder, og vi ellers ville kunne risikere at bruker får for mye penger. Se TS-Docs
                // for eksempler.
                Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR -> tidligsteEndring.minusDays(29)
                else -> tidligsteEndring
            }
    }
}
