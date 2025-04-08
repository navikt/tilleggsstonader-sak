package no.nav.tilleggsstonader.sak.oppfølging.kontroller

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.oppfølging.Kontroll
import no.nav.tilleggsstonader.sak.oppfølging.ÅrsakKontroll
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate

object OppfølgingKontrollerUtil {
    fun finnEndringFomTom(
        vedtaksperiode: Vedtaksperiode,
        register: Periode<LocalDate>,
    ): MutableList<Kontroll> =
        mutableListOf<Kontroll>().apply {
            if (register.fom > vedtaksperiode.fom) {
                add(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = register.fom))
            }
            if (register.tom < vedtaksperiode.tom) {
                add(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = register.tom))
            }
        }
}
