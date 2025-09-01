package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.kanMålgruppeBrukesForStønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

fun kanYtelseBrukesIBehandling(
    stønadstype: Stønadstype,
    ytelse: PeriodeGrunnlagYtelse,
): Boolean {
    if (ytelse.subtype == PeriodeGrunnlagYtelse.YtelseSubtype.AAP_FERDIG_AVKLART) {
        return false
    }
    return kanMålgruppeBrukesForStønad(stønadstype, ytelse.type.tilMålgruppe())
}

fun TypeYtelsePeriode.tilMålgruppe() =
    when (this) {
        TypeYtelsePeriode.AAP -> MålgruppeType.AAP
        TypeYtelsePeriode.DAGPENGER -> MålgruppeType.DAGPENGER
        TypeYtelsePeriode.ENSLIG_FORSØRGER -> MålgruppeType.OVERGANGSSTØNAD
        TypeYtelsePeriode.OMSTILLINGSSTØNAD -> MålgruppeType.OMSTILLINGSSTØNAD
        TypeYtelsePeriode.TILTAKSPENGER -> MålgruppeType.TILTAKSPENGER
    }
