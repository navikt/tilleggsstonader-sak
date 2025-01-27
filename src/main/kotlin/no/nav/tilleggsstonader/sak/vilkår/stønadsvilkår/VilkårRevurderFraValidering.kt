package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import java.time.LocalDate

object VilkårRevurderFraValidering {
    fun validerNyPeriodeRevurdering(
        behandling: Saksbehandling,
        periode: Vilkår,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom != null && periode.fom < behandling.revurderFra) {
            "Kan ikke opprette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerSlettPeriodeRevurdering(
        behandling: Saksbehandling,
        periode: Vilkår,
    ) {
        feilHvis(behandling.revurderFra != null && periode.fom != null && periode.fom < behandling.revurderFra) {
            "Kan ikke slette ${periodeInfo(behandling, periode.fom)}"
        }
    }

    fun validerEndrePeriodeRevurdering(
        behandling: Saksbehandling,
        eksisterendePeriode: Vilkår,
        oppdatertPeriode: Vilkår,
    ) {
        val revurderFra = behandling.revurderFra ?: return

        if (eksisterendePeriode.fom == null || eksisterendePeriode.fom >= revurderFra) {
            feilHvis(oppdatertPeriode.fom != null && oppdatertPeriode.fom < revurderFra) {
                "Kan ikke sette fom før revurderingsdato ${revurderFra.norskFormat()}"
            }
            return
        }

        feilHvis(
            eksisterendePeriode.resultat != oppdatertPeriode.resultat ||
                eksisterendePeriode.utgift != oppdatertPeriode.utgift ||
                ulikDelvilkårsett(eksisterendePeriode, oppdatertPeriode) ||
                eksisterendePeriode.fom != oppdatertPeriode.fom ||
                oppdatertPeriode.tom == null ||
                oppdatertPeriode.tom < revurderFra.minusDays(1),
        ) {
            secureLogger.info(
                "Ugyldig endring på periode eksisterendePeriode=$eksisterendePeriode" +
                    " oppdatertPeriode=$oppdatertPeriode",
            )
            "Ugyldig endring på ${periodeInfo(behandling, eksisterendePeriode.fom)}"
        }
    }

    private fun ulikDelvilkårsett(
        eksisterendePeriode: Vilkår,
        oppdatertPeriode: Vilkår,
    ) = eksisterendePeriode.delvilkårsett.ignorerBegrunnelse() != oppdatertPeriode.delvilkårsett.ignorerBegrunnelse()

    private fun List<Delvilkår>.ignorerBegrunnelse() =
        this.map { delvilkår ->
            delvilkår.copy(vurderinger = delvilkår.vurderinger.map { it.copy(begrunnelse = null) })
        }

    private fun periodeInfo(
        behandling: Saksbehandling,
        fomVilkårperiode: LocalDate?,
    ) = "periode som begynner(${fomVilkårperiode?.norskFormat()}) før revurderingsdato(${behandling.revurderFra?.norskFormat()})"
}
