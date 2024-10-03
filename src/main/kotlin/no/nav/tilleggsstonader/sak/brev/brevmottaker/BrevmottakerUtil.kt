package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke

object BrevmottakerUtil {

    fun validerUnikeBrevmottakere(brevmottakereDto: BrevmottakereDto) {
        val personmottakerIdenter = brevmottakereDto.personer.map { it.personIdent }
        brukerfeilHvisIkke(personmottakerIdenter.distinct().size == personmottakerIdenter.size) {
            "En person kan bare legges til en gang som brevmottaker"
        }

        val organisasjonsmottakerIdenter = brevmottakereDto.organisasjoner.map { it.organisasjonsnummer }
        brukerfeilHvisIkke(organisasjonsmottakerIdenter.distinct().size == organisasjonsmottakerIdenter.size) {
            "En organisasjon kan bare legges til en gang som brevmottaker"
        }
    }

    fun validerAntallBrevmottakere(brevmottakere: BrevmottakereDto) {
        val antallPersonmottakere = brevmottakere.personer.size
        val antallOrganisasjonMottakere = brevmottakere.organisasjoner.size
        val antallMottakere = antallPersonmottakere + antallOrganisasjonMottakere
        brukerfeilHvis(antallMottakere == 0) {
            "Vedtaksbrevet må ha minst 1 mottaker"
        }
        brukerfeilHvis(antallMottakere > 2) {
            "Vedtaksbrevet kan ikke ha mer enn 2 mottakere"
        }
    }
}
