package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

fun no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn.tilNavn() =
    Navn(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
    )
