package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeEllerAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import java.time.LocalDate

object VilkårperiodeRevurderFraValidering {

    fun validerNyPeriodeRevurdering(
        behandling: Saksbehandling,
        periode: LagreVilkårperiode,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom < behandling.revurderFra) {
            "Kan ikke opprette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerSlettPeriodeRevurdering(
        behandling: Saksbehandling,
        periode: MålgruppeEllerAktivitet,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom < behandling.revurderFra) {
            "Kan ikke slette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerEndrePeriodeRevurdering(
        behandling: Saksbehandling,
        eksisterendePeriode: MålgruppeEllerAktivitet,
        oppdatertPeriode: MålgruppeEllerAktivitet,
    ) {
        val revurderFra = behandling.revurderFra ?: return

        if (eksisterendePeriode.fom >= revurderFra) {
            feilHvis(oppdatertPeriode.fom < revurderFra) {
                "Kan ikke sette fom før revurderingsdato ${revurderFra.norskFormat()}"
            }
            return
        }

        feilHvis(
            eksisterendePeriode.resultat != oppdatertPeriode.resultat ||
                eksisterendePeriode.delvilkår != oppdatertPeriode.delvilkår ||
                eksisterendePeriode.aktivitetsdager != oppdatertPeriode.aktivitetsdager ||
                eksisterendePeriode.fom != oppdatertPeriode.fom ||
                oppdatertPeriode.tom < revurderFra.minusDays(1),
        ) {
            secureLogger.info(
                "Ugyldig endring på vilkårperiode eksisterendePeriode=$eksisterendePeriode" +
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
