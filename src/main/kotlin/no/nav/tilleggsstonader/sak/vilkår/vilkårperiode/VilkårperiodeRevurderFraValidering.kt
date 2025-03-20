package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AldersvilkårVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderingOrThrow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAPLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevneLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygdLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.Vurderinger
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
                "Kan ikke sette fom før revurder-fra(${revurderFra.norskFormat()})"
            }
            return
        }

        feilHvis(eksisterendePeriode.fom != oppdatertPeriode.fom) {
            "Kan ikke endre fom til ${oppdatertPeriode.fom.norskFormat()} fordi " +
                "revurder fra(${revurderFra.norskFormat()}) er før. Kontakt utviklingsteamet"
        }
        feilHvis(oppdatertPeriode.tom < revurderFra.minusDays(1)) {
            "Kan ikke sette tom tidligere enn dagen før revurder-fra(${revurderFra.norskFormat()})"
        }
        feilHvis(eksisterendePeriode.faktaOgVurdering.type != oppdatertPeriode.faktaOgVurdering.type) {
            logEndring(eksisterendePeriode, oppdatertPeriode)
            "Kan ikke endre type på perioden. Kontakt utviklingsteamet"
        }
        feilHvis(eksisterendePeriode.faktaOgVurdering.fakta != oppdatertPeriode.faktaOgVurdering.fakta) {
            logEndring(eksisterendePeriode, oppdatertPeriode)
            "Kan ikke endre fakta på perioden. Kontakt utviklingsteamet"
        }
        val eksisterendeVurderinger = eksisterendeVurderinger(eksisterendePeriode, oppdatertPeriode)
        feilHvis(eksisterendeVurderinger != oppdatertPeriode.faktaOgVurdering.vurderinger) {
            logEndring(eksisterendePeriode, oppdatertPeriode)
            "Kan ikke endre vurderinger på perioden. Kontakt utviklingsteamet"
        }
        feilHvis(eksisterendePeriode.resultat != oppdatertPeriode.resultat) {
            logEndring(eksisterendePeriode, oppdatertPeriode)
            "Resultat kan ikke endre seg. Kontakt utviklingsteamet"
        }
    }

    private fun logEndring(
        eksisterendePeriode: Vilkårperiode,
        oppdatertPeriode: Vilkårperiode,
    ) {
        secureLogger.info(
            "Ugyldig endring på vilkårperiode " +
                "eksisterendePeriode=$eksisterendePeriode oppdatertPeriode=$oppdatertPeriode",
        )
    }

    private fun eksisterendeVurderinger(
        eksisterendePeriode: Vilkårperiode,
        oppdatertPeriode: Vilkårperiode,
    ): Vurderinger =
        eksisterendePeriode.faktaOgVurdering.vurderinger.let { vurderinger ->
            if (vurderinger is AldersvilkårVurdering) {
                brukNyttAldersvilkår(vurderinger, oppdatertPeriode)
            } else {
                vurderinger
            }
        }

    /**
     * Overskriver aldersvilkår med oppdatert informasjon då aldervilkåret automatisk vurderes og skal kunne gå fra
     * [SvarJaNei.GAMMEL_MANGLER_DATA] til [SvarJaNei.JA] uten at man kaster feil
     */
    private fun brukNyttAldersvilkår(
        vurdering: AldersvilkårVurdering,
        oppdatertPeriode: Vilkårperiode,
    ): MedlemskapVurdering {
        val oppdatertAldersvilkår =
            oppdatertPeriode.faktaOgVurdering.vurderinger
                .takeIfVurderingOrThrow<AldersvilkårVurdering>()
                .aldersvilkår
        return when (vurdering) {
            is VurderingAAP -> vurdering.copy(aldersvilkår = oppdatertAldersvilkår)
            is VurderingNedsattArbeidsevne -> vurdering.copy(aldersvilkår = oppdatertAldersvilkår)
            is VurderingOmstillingsstønad -> vurdering.copy(aldersvilkår = oppdatertAldersvilkår)
            is VurderingUføretrygd -> vurdering.copy(aldersvilkår = oppdatertAldersvilkår)
            is VurderingAAPLæremidler -> vurdering.copy(aldersvilkår = oppdatertAldersvilkår)
            is VurderingUføretrygdLæremidler -> vurdering.copy(aldersvilkår = oppdatertAldersvilkår)
            is VurderingNedsattArbeidsevneLæremidler -> vurdering.copy(aldersvilkår = oppdatertAldersvilkår)
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
