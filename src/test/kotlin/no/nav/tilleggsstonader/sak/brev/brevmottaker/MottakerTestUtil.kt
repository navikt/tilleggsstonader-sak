package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.Mottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerType

object MottakerTestUtil {

    fun mottakerPerson(ident: String, mottakerRolle: MottakerRolle = MottakerRolle.BRUKER) = Mottaker(
        ident = ident,
        mottakerRolle = mottakerRolle,
        mottakerType = MottakerType.PERSON,
    )
}
