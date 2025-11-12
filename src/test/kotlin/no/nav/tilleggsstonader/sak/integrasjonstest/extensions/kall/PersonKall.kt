package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.fagsak.søk.Søkeresultat
import no.nav.tilleggsstonader.sak.infrastruktur.felles.PersonIdentDto
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class PersonKall(
    private val testklient: Testklient,
) {
    fun fagsakEkstern(eksternFagsakId: Long): Søkeresultat = apiRespons.fagsakEkstern(eksternFagsakId).expectOkWithBody()

    fun sok(personIdent: String): Søkeresultat = apiRespons.sok(personIdent).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = PersonApi()

    inner class PersonApi {
        fun fagsakEkstern(eksternFagsakId: Long) = testklient.get("/api/sok/person/fagsak-ekstern/$eksternFagsakId")

        fun sok(personIdent: String) = testklient.post("/api/sok", PersonIdentDto(personIdent = personIdent))
    }
}
