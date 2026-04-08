package no.nav.tilleggsstonader.sak.vedtak

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
            return Beregningsplan(omfang = Beregningsomfang.ALLE_PERIODER)
        }
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(saksbehandling.id, vedtaksperioder)
        return if (tidligsteEndring != null) {
            Beregningsplan(
                omfang = Beregningsomfang.FRA_DATO,
                fraDato = tidligsteEndring,
            )
        } else {
            Beregningsplan(omfang = Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT)
        }
    }

    fun utledForOpphør(opphørsdato: LocalDate): Beregningsplan = Beregningsplan(omfang = Beregningsomfang.FRA_DATO, fraDato = opphørsdato)
}
