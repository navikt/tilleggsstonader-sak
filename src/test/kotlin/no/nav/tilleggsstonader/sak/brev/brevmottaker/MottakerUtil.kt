package no.nav.tilleggsstonader.sak.brev.brevmottaker

object MottakerUtil {

    fun mottakerPerson(ident: String, mottakerRolle: MottakerRolle = MottakerRolle.BRUKER) = Mottaker(
        ident = ident,
        mottakerRolle = mottakerRolle,
        mottakerType = MottakerType.PERSON,
    )
}
