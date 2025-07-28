package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode

object YtelserUtil {
    fun finnRelevanteYtelsesTyper(type: Stønadstype) =
        when (type) {
            Stønadstype.BARNETILSYN,
            Stønadstype.LÆREMIDLER,
            Stønadstype.BOUTGIFTER,
            Stønadstype.DAGLIG_REISE_TSO,
            Stønadstype.DAGLIG_REISE_TSR,
            ->
                listOf(
                    TypeYtelsePeriode.AAP,
                    TypeYtelsePeriode.ENSLIG_FORSØRGER,
                    TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                )

            else -> error("Finner ikke relevante ytelser for stønadstype $type")
        }
}
