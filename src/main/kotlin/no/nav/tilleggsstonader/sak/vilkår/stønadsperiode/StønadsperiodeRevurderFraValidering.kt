package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import java.time.LocalDate

object StønadsperiodeRevurderFraValidering {

    fun validerNyPeriodeRevurdering(
        behandling: Saksbehandling,
        periode: Stønadsperiode,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom < behandling.revurderFra) {
            "Kan ikke opprette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerSlettPeriodeRevurdering(
        behandling: Saksbehandling,
        periode: Stønadsperiode,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom < behandling.revurderFra) {
            "Kan ikke slette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerEndrePeriodeRevurdering(
        behandling: Saksbehandling,
        eksisterendePeriode: Stønadsperiode,
        oppdatertPeriode: Stønadsperiode,
    ) {
        val revurderFra = behandling.revurderFra ?: return

        if (eksisterendePeriode.fom >= revurderFra) {
            feilHvis(oppdatertPeriode.fom < revurderFra) {
                "Kan ikke sette fom før revurderingsdato ${revurderFra.norskFormat()}"
            }
            return
        }

        feilHvis(
            eksisterendePeriode.fom != oppdatertPeriode.fom ||
                eksisterendePeriode.målgruppe != oppdatertPeriode.målgruppe ||
                eksisterendePeriode.aktivitet != oppdatertPeriode.aktivitet ||
                ugyldigEndringAvTom(eksisterendePeriode, oppdatertPeriode, revurderFra),
        ) {
            secureLogger.info(
                "Ugyldig endring på stønadsperiode eksisterendePeriode=$eksisterendePeriode" +
                    " oppdatertPeriode=$oppdatertPeriode",
            )
            "Ugyldig endring på ${periodeInfo(behandling, eksisterendePeriode.fom)}"
        }
    }

    /**
     * Kan endre på tom hvis tom er 2 dager før revurderFra
     * Kan endre på tom frem i tiden hvis tom er dagen før revurderFra
     */
    private fun ugyldigEndringAvTom(
        eksisterendePeriode: Stønadsperiode,
        oppdatertPeriode: Stønadsperiode,
        revurderFra: LocalDate,
    ): Boolean {
        return if (eksisterendePeriode.tom < revurderFra.minusDays(1)) {
            eksisterendePeriode.tom != oppdatertPeriode.tom
        } else {
            oppdatertPeriode.tom < revurderFra.minusDays(1)
        }
    }

    private fun periodeInfo(
        behandling: Saksbehandling,
        fomVilkårperiode: LocalDate,
    ) =
        "periode som begynner(${fomVilkårperiode.norskFormat()}) før revurderingsdato(${behandling.revurderFra?.norskFormat()})"
}
