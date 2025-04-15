package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

object BehandlingOppsummeringUtil {
    fun <P> List<P>.filtrerOgDelFraRevurderFra(revurderFra: LocalDate?): List<P> where P : Periode<LocalDate>, P : KopierPeriode<P> {
        if (revurderFra == null) return this

        return this.mapNotNull { periode ->
            when {
                periode.tom < revurderFra -> null
                periode.fom < revurderFra -> periode.medPeriode(revurderFra, periode.tom)
                else -> periode
            }
        }
    }
}
