package no.nav.tilleggsstonader.sak.brev.brevmottaker

import java.util.UUID

data class BrevmottakereDto(
    val personer: List<BrevmottakerPersonDto>,
    val organisasjoner: List<BrevmottakerOrganisasjonDto>,
)

data class BrevmottakerPersonDto(
    val id: UUID,
    val personIdent: String,
    val mottakerRolle: MottakerRolle,
)

data class BrevmottakerOrganisasjonDto(
    val id: UUID,
    val organisasjonsnummer: String,
    val navnHosOrganisasjon: String,
    val mottakerRolle: MottakerRolle,
)

fun Brevmottaker.tilPersonDto(): BrevmottakerPersonDto =
    BrevmottakerPersonDto(id = id, personIdent = ident, mottakerRolle = mottakerRolle)

fun Brevmottaker.tilOrganisasjonDto(): BrevmottakerOrganisasjonDto =
    BrevmottakerOrganisasjonDto(
        id = id,
        organisasjonsnummer = ident,
        navnHosOrganisasjon = navnHosOrganisasjon ?: error("Navn hos organisasjon er påkrevd"),
        mottakerRolle = mottakerRolle,
    )

fun List<Brevmottaker>.tilBrevmottakereDto(): BrevmottakereDto =
    BrevmottakereDto(
        personer = this.mapNotNull { if (it.mottakerType == MottakerType.PERSON) it.tilPersonDto() else null },
        organisasjoner = this.mapNotNull { if (it.mottakerType == MottakerType.ORGANISASJON) it.tilOrganisasjonDto() else null },
    )
