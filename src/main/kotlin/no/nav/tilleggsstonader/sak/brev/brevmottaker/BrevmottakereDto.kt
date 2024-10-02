package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerOrganisasjonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerPersonDto
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.brevmottaker.MottakerRolle as MottakerRolleKontrakt

data class BrevmottakereDto(
    val personer: List<BrevmottakerPersonDto>,
    val organisasjoner: List<BrevmottakerOrganisasjonDto>,
)

fun Mottaker.tilPersonDto(id: UUID): BrevmottakerPersonDto =
    BrevmottakerPersonDto(
        id = id,
        personIdent = ident,
        mottakerRolle = MottakerRolleKontrakt.valueOf(mottakerRolle.name),
        navn = mottakerNavn,
    )

fun Mottaker.tilOrganisasjonDto(id: UUID): BrevmottakerOrganisasjonDto =
    BrevmottakerOrganisasjonDto(
        id = id,
        organisasjonsnummer = ident,
        navnHosOrganisasjon = mottakerNavn ?: error("Navn hos organisasjon er påkrevd"),
        organisasjonsnavn = organisasjonsNavn.orEmpty(),
    )

fun List<BrevmottakerVedtaksbrev>.tilBrevmottakereDto(): BrevmottakereDto =
    BrevmottakereDto(
        personer = this.mapNotNull {
            if (it.mottaker.mottakerType == MottakerType.PERSON) it.mottaker.tilPersonDto(it.id) else null
        },
        organisasjoner = this.mapNotNull {
            if (it.mottaker.mottakerType == MottakerType.ORGANISASJON) it.mottaker.tilOrganisasjonDto(it.id) else null
        },
    )

fun BrevmottakerOrganisasjonDto.tilMottaker() = Mottaker(
    mottakerRolle = MottakerRolle.FULLMAKT,
    mottakerType = MottakerType.ORGANISASJON,
    ident = organisasjonsnummer,
    mottakerNavn = navnHosOrganisasjon,
    organisasjonsNavn = organisasjonsnavn,
)

fun BrevmottakerPersonDto.tilMottaker() = Mottaker(
    mottakerRolle = MottakerRolle.valueOf(mottakerRolle.name),
    mottakerType = MottakerType.PERSON,
    ident = personIdent,
    mottakerNavn = navn,
)
