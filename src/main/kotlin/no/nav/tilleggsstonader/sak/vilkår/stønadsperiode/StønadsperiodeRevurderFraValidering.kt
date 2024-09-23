package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import java.time.LocalDate

object StønadsperiodeRevurderFraValidering {

    fun validerNyPeriodeIForholdTilRevurderFra(
        behandling: Saksbehandling,
        periode: Stønadsperiode,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom < behandling.revurderFra) {
            "Kan ikke opprette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerSlettPeriodeIForholdTilRevurderFra(
        behandling: Saksbehandling,
        periode: Stønadsperiode,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom < behandling.revurderFra) {
            "Kan ikke slette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerEndrePeriodeIForholdTilRevurderFra(
        behandling: Saksbehandling,
        eksisterendePeriode: Stønadsperiode,
        oppdatertPeriode: Stønadsperiode,
    ) {
        val revurderFra = behandling.revurderFra
        if (revurderFra == null || revurderFra > eksisterendePeriode.tom) {
            return
        }
        feilHvis(
            eksisterendePeriode.fom != oppdatertPeriode.fom ||
                eksisterendePeriode.målgruppe != oppdatertPeriode.målgruppe ||
                eksisterendePeriode.aktivitet != oppdatertPeriode.aktivitet ||
                oppdatertPeriode.tom < revurderFra.minusDays(1),
        ) {
            secureLogger.info(
                "Ugyldig endring på stønadsperiode eksisterendePeriode=$eksisterendePeriode" +
                    " oppdatertPeriode=$oppdatertPeriode",
            )
            "Ugyldig endring på ${periodeInfo(behandling, eksisterendePeriode.fom)}"
        }
    }

    private fun periodeInfo(
        behandling: Saksbehandling,
        fomVilkårperiode: LocalDate,
    ) =
        "periode som begynner(${fomVilkårperiode.norskFormat()}) før revurderingsdato(${behandling.revurderFra?.norskFormat()})"
}
