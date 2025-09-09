package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode

object YtelserUtil {
    fun finnRelevanteYtelsesTyper(type: Stønadstype) =
        when (type) {
            Stønadstype.BARNETILSYN,
            Stønadstype.LÆREMIDLER,
            Stønadstype.BOUTGIFTER,
            ->
                listOf(
                    TypeYtelsePeriode.AAP,
                    TypeYtelsePeriode.ENSLIG_FORSØRGER,
                    TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                )

            Stønadstype.DAGLIG_REISE_TSO,
            Stønadstype.DAGLIG_REISE_TSR,
            ->
                listOf(
                    TypeYtelsePeriode.AAP,
                    TypeYtelsePeriode.ENSLIG_FORSØRGER,
                    TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                    TypeYtelsePeriode.TILTAKSPENGER,
                    TypeYtelsePeriode.DAGPENGER,
                )
        }
}
