package no.nav.tilleggsstonader.sak.brev.brevmottaker.domain

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis

data class Mottaker(
    val mottakerRolle: MottakerRolle,
    val mottakerType: MottakerType,
    val ident: String,
    val mottakerNavn: String? = null,
    val organisasjonsNavn: String? = null,
) {
    init {
        feilHvis(mottakerType == MottakerType.ORGANISASJON && mottakerNavn.isNullOrBlank()) {
            "Navn hos organisasjon er påkrevd"
        }

        feilHvis(mottakerRolle == MottakerRolle.BRUKER && mottakerType == MottakerType.ORGANISASJON) {
            "Ugyldig kombinasjon av mottakerType og mottakerRolle. Bruker/søker kan ikke være organisasjon."
        }
    }
}

enum class MottakerRolle {
    BRUKER,
    VERGE,
    FULLMAKT,
}

enum class MottakerType {
    PERSON,
    ORGANISASJON,
}
