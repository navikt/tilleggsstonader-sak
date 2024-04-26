package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class ArbeidsfordelingService(
    @Qualifier("shortCache")
    private val cacheManager: CacheManager,
) {

    companion object {
        const val MASKINELL_JOURNALFOERENDE_ENHET = "9999"
        val ENHET_NASJONAL_NAY = Arbeidsfordelingsenhet("4462", "Tilleggsstønad INN")
    }

    fun hentNavEnhetId(ident: String, oppgavetype: Oppgavetype) = when (oppgavetype) {
        Oppgavetype.VurderHenvendelse -> hentNavEnhetForOppfølging(ident, oppgavetype)?.enhetId
        else -> hentNavEnhet(ident)?.enhetId
    }

    fun hentNavEnhet(ident: String): Arbeidsfordelingsenhet? {
        return ENHET_NASJONAL_NAY // TODO: Implementer arbeidsfordeling
        /*
        return cacheManager.getNullable("navEnhet", ident) {
            arbeidsfordelingClient.hentNavEnhetForPersonMedRelasjoner(ident).firstOrNull()
        }
         */
    }

    fun hentNavEnhetForOppfølging(ident: String, oppgavetype: Oppgavetype): Arbeidsfordelingsenhet? {
        return ENHET_NASJONAL_NAY // TODO: Implementer arbeidsfordeling
    }
        /*
        cacheManager.getNullable("navEnhetForOppfølging", ident) {
            arbeidsfordelingClient.hentBehandlendeEnhetForOppfølging(ident)
                ?: error("Fant ikke NAV-enhet for oppgave av type $oppgavetype")
        }
         */

    fun hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent: String): String {
        return hentNavEnhet(personIdent)?.enhetId ?: MASKINELL_JOURNALFOERENDE_ENHET
    }
}
