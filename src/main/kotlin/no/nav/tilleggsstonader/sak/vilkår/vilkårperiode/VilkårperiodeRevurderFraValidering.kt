package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import java.time.LocalDate
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object VilkårperiodeRevurderFraValidering {
    fun validerNyPeriodeRevurdering(
        behandling: Saksbehandling,
        fom: LocalDate,
    ) {
        if (behandling.type == BehandlingType.REVURDERING) {
            validerRevurderFraErSatt(behandling.revurderFra)
            feilHvis(fom < behandling.revurderFra) {
                "Kan ikke opprette ${periodeInfo(behandling, fom)}"
            }
        }
    }

    fun validerSlettPeriodeRevurdering(
        behandling: Saksbehandling,
        periode: Vilkårperiode,
    ) {
        if (behandling.type == BehandlingType.REVURDERING) {
            validerRevurderFraErSatt(behandling.revurderFra)
            feilHvis(periode.fom < behandling.revurderFra) {
                "Kan ikke slette ${periodeInfo(behandling, periode.fom)}"
            }
        }
    }

    fun validerEndrePeriodeRevurdering(
        behandling: Saksbehandling,
        eksisterendePeriode: Vilkårperiode,
        oppdatertPeriode: Vilkårperiode,
    ) {
        if (behandling.type != BehandlingType.REVURDERING) {
            return
        }

        val revurderFra = behandling.revurderFra

        validerRevurderFraErSatt(revurderFra)

        if (eksisterendePeriode.fom >= revurderFra) {
            feilHvis(oppdatertPeriode.fom < revurderFra) {
                "Kan ikke sette fom før revurderingsdato ${revurderFra.norskFormat()}"
            }
            return
        }

        feilHvis(
            eksisterendePeriode.resultat != oppdatertPeriode.resultat ||
                eksisterendePeriode.faktaOgVurdering != oppdatertPeriode.faktaOgVurdering ||
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

    @OptIn(ExperimentalContracts::class)
    private fun validerRevurderFraErSatt(revurderFra: LocalDate?) {
        contract {
            returns() implies (revurderFra != null)
        }
        brukerfeilHvis(revurderFra == null) {
            "Revurder fra-dato må settes før du kan gjøre endringer på inngangvilkårene"
        }
    }

    private fun periodeInfo(
        behandling: Saksbehandling,
        fomVilkårperiode: LocalDate,
    ) = "periode som begynner(${fomVilkårperiode.norskFormat()}) før revurderingsdato(${behandling.revurderFra?.norskFormat()})"
}
