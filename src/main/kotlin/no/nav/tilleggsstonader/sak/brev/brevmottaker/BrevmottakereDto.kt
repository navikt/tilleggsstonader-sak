package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerOrganisasjonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerPersonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.MottakerRolle

data class BrevmottakereDto(
    val personer: List<BrevmottakerPersonDto>,
    val organisasjoner: List<BrevmottakerOrganisasjonDto>,
)

fun Brevmottaker.tilPersonDto(): BrevmottakerPersonDto =
    BrevmottakerPersonDto(id = id, personIdent = ident, mottakerRolle = MottakerRolle.valueOf(mottakerRolle.name), navn = mottakerNavn)

fun Brevmottaker.tilOrganisasjonDto(): BrevmottakerOrganisasjonDto =
    BrevmottakerOrganisasjonDto(
        id = id,
        organisasjonsnummer = ident,
        navnHosOrganisasjon = mottakerNavn ?: error("Navn hos organisasjon er påkrevd"),
        organisasjonsnavn = organisasjonsNavn.orEmpty(),

    )

fun List<Brevmottaker>.tilBrevmottakereDto(): BrevmottakereDto =
    BrevmottakereDto(
        personer = this.mapNotNull { if (it.mottakerType == MottakerType.PERSON) it.tilPersonDto() else null },
        organisasjoner = this.mapNotNull { if (it.mottakerType == MottakerType.ORGANISASJON) it.tilOrganisasjonDto() else null },
    )
