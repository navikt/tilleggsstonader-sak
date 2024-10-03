package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.Mottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerType

object MottakerUtil {

    fun Mottaker.tilAvsenderMottaker() = AvsenderMottaker(
        id = ident,
        idType = when (mottakerType) {
            MottakerType.PERSON -> BrukerIdType.FNR
            MottakerType.ORGANISASJON -> BrukerIdType.ORGNR
        },
        navn = when (mottakerType) {
            MottakerType.PERSON -> null
            MottakerType.ORGANISASJON -> mottakerNavn
        },
    )
}
