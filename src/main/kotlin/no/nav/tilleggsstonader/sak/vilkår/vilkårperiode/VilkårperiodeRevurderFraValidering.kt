package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilFaktaOgSvarDto
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

    /**
     * Valider at det ikke er gjort noen endringer på vilkårperiode
     * Antar at resultatet ikke er endret dersom ingen svar er endret
     */
    fun validerAtKunTomErEndret(
        eksisterendePeriode: Vilkårperiode,
        oppdatertPeriode: LagreVilkårperiode,
        revurderFra: LocalDate,
    ) {
        feilHvis(eksisterendePeriode.fom != oppdatertPeriode.fom) {
            "Kan ikke endre fom til ${oppdatertPeriode.fom.norskFormat()} fordi " +
                "revurder fra(${revurderFra.norskFormat()}) er før. Kontakt utviklingsteamet"
        }
        feilHvis(oppdatertPeriode.tom < revurderFra.minusDays(1)) {
            "Kan ikke sette tom tidligere enn dagen før revurder-fra(${revurderFra.norskFormat()})"
        }
        feilHvis(eksisterendePeriode.faktaOgVurdering.tilFaktaOgSvarDto() != oppdatertPeriode.faktaOgSvar) {
            logEndring(eksisterendePeriode, oppdatertPeriode)
            "Kan ikke endre vurderinger eller fakta på perioden. Kontakt utviklingsteamet"
        }
    }

    private fun logEndring(
        eksisterendePeriode: Vilkårperiode,
        oppdatertPeriode: LagreVilkårperiode,
    ) {
        secureLogger.info(
            "Ugyldig endring på vilkårperiode " +
                "eksisterendePeriode=$eksisterendePeriode oppdatertPeriode=$oppdatertPeriode",
        )
    }

    @OptIn(ExperimentalContracts::class)
    fun validerRevurderFraErSatt(revurderFra: LocalDate?) {
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
