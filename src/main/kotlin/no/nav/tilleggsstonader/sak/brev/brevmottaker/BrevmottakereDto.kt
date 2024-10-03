package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerOrganisasjonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerPersonDto
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.Mottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerType
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

fun List<BrevmottakerVedtaksbrev>.tilBrevmottakereDto(): BrevmottakereDto = this
    .map { it.id to it.mottaker }
    .let(::brevmottakereDto)

private fun brevmottakereDto(mottakere: List<Pair<UUID, Mottaker>>) =
    BrevmottakereDto(
        personer = mottakere.mapPersoner(),
        organisasjoner = mottakere.mapOrganisasjoner(),
    )

private fun List<Pair<UUID, Mottaker>>.mapOrganisasjoner() = mapNotNull { (id, mottaker) ->
    if (mottaker.mottakerType == MottakerType.ORGANISASJON) mottaker.tilOrganisasjonDto(id) else null
}

private fun List<Pair<UUID, Mottaker>>.mapPersoner() = mapNotNull { (id, mottaker) ->
    if (mottaker.mottakerType == MottakerType.PERSON) mottaker.tilPersonDto(id) else null
}

fun BrevmottakerDto.tilMottaker() = when (this) {
    is BrevmottakerOrganisasjonDto -> tilMottaker()
    is BrevmottakerPersonDto -> tilMottaker()
}

private fun BrevmottakerOrganisasjonDto.tilMottaker() = Mottaker(
    mottakerRolle = MottakerRolle.FULLMAKT,
    mottakerType = MottakerType.ORGANISASJON,
    ident = organisasjonsnummer,
    mottakerNavn = navnHosOrganisasjon,
    organisasjonsNavn = organisasjonsnavn,
)

private fun BrevmottakerPersonDto.tilMottaker() = Mottaker(
    mottakerRolle = MottakerRolle.valueOf(mottakerRolle.name),
    mottakerType = MottakerType.PERSON,
    ident = personIdent,
    mottakerNavn = navn,
)
