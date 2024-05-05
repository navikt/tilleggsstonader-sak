package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningDto
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningType
import no.nav.tilleggsstonader.sak.infrastruktur.config.getNullable
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.tilDiskresjonskode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class ArbeidsfordelingService(
    @Qualifier("shortCache")
    private val cacheManager: CacheManager,
    private val arbeidsfordelingClient: ArbeidsfordelingClient,
    private val personService: PersonService,
    private val egenAnsattService: EgenAnsattService,
) {

    companion object {
        const val MASKINELL_JOURNALFOERENDE_ENHET = "9999"
        val ENHET_NASJONAL_NAY = Arbeidsfordelingsenhet("4462", "Tilleggsstønad INN")
    }

    fun hentNavEnhetId(ident: String, oppgavetype: Oppgavetype, tema: Tema = Tema.TSO) = when (oppgavetype) {
        Oppgavetype.VurderHenvendelse -> hentNavEnhetForOppfølging(ident, oppgavetype)?.enhetId
        else -> hentNavEnhet(ident, tema)?.enhetId
    }

    fun hentNavEnhet(ident: String, tema: Tema = Tema.TSO): Arbeidsfordelingsenhet? {
        return cacheManager.getNullable("navEnhet", ident) {
            arbeidsfordelingClient.finnArbeidsfordelingsenhet(lagArbeidsfordelingKritierieForPerson(ident, tema)).firstOrNull()
        }
    }

    fun hentNavEnhetForOppfølging(ident: String, oppgavetype: Oppgavetype, tema: Tema = Tema.TSO): Arbeidsfordelingsenhet? {
        return cacheManager.getNullable("navEnhetForOppfølging", ident) {
            arbeidsfordelingClient.finnArbeidsfordelingsenhet(lagArbeidsfordelingKritierieForPerson(ident, tema, oppgavetype)).firstOrNull()
                ?: error("Fant ikke NAV-enhet for oppgave av type $oppgavetype")
        }
    }

    fun hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent: String): String {
        return hentNavEnhet(personIdent)?.enhetId ?: MASKINELL_JOURNALFOERENDE_ENHET
    }

    private fun lagArbeidsfordelingKritierieForPerson(
        personIdent: String,
        arbeidsfordelingstema: Tema,
        oppgavetype: Oppgavetype? = null,
    ): ArbeidsfordelingKriterie {
        val personinfo = personService.hentSøker(personIdent)
        val geografiskTilknytning = utledGeografiskTilknytningKode(personService.hentGeografiskTilknytning(personIdent))
        val diskresjonskode = personinfo.adressebeskyttelse.singleOrNull()?.gradering?.tilDiskresjonskode()

        return ArbeidsfordelingKriterie(
            tema = arbeidsfordelingstema.name,
            diskresjonskode = diskresjonskode,
            geografiskOmraade = geografiskTilknytning,
            skjermet = egenAnsattService.erEgenAnsatt(personIdent),
            oppgavetype = oppgavetype,
        )
    }

    private fun utledGeografiskTilknytningKode(geografiskTilknytning: GeografiskTilknytningDto?): String? {
        return geografiskTilknytning?.let {
            when (it.gtType) {
                GeografiskTilknytningType.BYDEL -> it.gtBydel
                GeografiskTilknytningType.KOMMUNE -> it.gtKommune
                GeografiskTilknytningType.UTLAND -> null
                GeografiskTilknytningType.UDEFINERT -> null
            }
        }
    }
}
