package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.sak.integrasjonstest.OpprettAvslag
import no.nav.tilleggsstonader.sak.integrasjonstest.OpprettInnvilgelse
import no.nav.tilleggsstonader.sak.integrasjonstest.OpprettOpphør
import no.nav.tilleggsstonader.sak.integrasjonstest.OpprettVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import java.time.LocalDate

@BehandlingTestdataDslMarker
class OpprettVedtakTestdataDsl {
    var vedtak: OpprettVedtak = OpprettInnvilgelse

    fun innvilgelse() {
        vedtak = OpprettInnvilgelse
    }

    fun opphør(
        årsaker: List<ÅrsakOpphør> = listOf(ÅrsakOpphør.ANNET),
        begrunnelse: String = "annet",
        opphørsdato: LocalDate,
    ) {
        vedtak = OpprettOpphør(årsaker, begrunnelse, opphørsdato)
    }

    fun avslag() {
        vedtak = OpprettAvslag
    }
}
