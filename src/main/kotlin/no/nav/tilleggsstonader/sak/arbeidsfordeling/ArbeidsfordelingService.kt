package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningDto
import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningType
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.infrastruktur.config.getNullable
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.tilDiskresjonskode
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MASKINELL_JOURNALFOERENDE_ENHET = "9999"
        const val GEOGRAFISK_TILKNYTTING_OSLO = "0301"
    }

    fun hentNavEnhetId(
        ident: String,
        oppgavetype: Oppgavetype,
        tema: Tema = Tema.TSO,
    ) = when (oppgavetype) {
        Oppgavetype.VurderHenvendelse -> hentNavEnhetForOppfølging(ident, oppgavetype)?.enhetNr
        else -> hentNavEnhet(ident, tema)?.enhetNr
    }

    fun hentNavEnhet(
        ident: String,
        tema: Tema = Tema.TSO,
    ): Arbeidsfordelingsenhet? =
        cacheManager.getNullable("navEnhet", ident) {
            val kriterie = lagArbeidsfordelingKritierieForPerson(ident, tema)
            val enheter = arbeidsfordelingClient.finnArbeidsfordelingsenhet(kriterie)
            if (enheter.size != 1) {
                logger.warn("Fant enheter=$enheter for $kriterie")
            }
            enheter.firstOrNull()
        }

    fun hentNavEnhetForOppfølging(
        ident: String,
        oppgavetype: Oppgavetype,
        tema: Tema = Tema.TSO,
    ): Arbeidsfordelingsenhet? =
        cacheManager.getNullable("navEnhetForOppfølging", ident) {
            arbeidsfordelingClient.finnArbeidsfordelingsenhet(lagArbeidsfordelingKritierieForPerson(ident, tema, oppgavetype)).firstOrNull()
                ?: error("Fant ikke Nav-enhet for oppgave av type $oppgavetype")
        }

    fun hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent: String): String =
        hentNavEnhet(personIdent)?.enhetNr ?: MASKINELL_JOURNALFOERENDE_ENHET

    private fun lagArbeidsfordelingKritierieForPerson(
        personIdent: String,
        arbeidsfordelingstema: Tema,
        oppgavetype: Oppgavetype? = null,
    ): ArbeidsfordelingKriterie {
        val personinfo = personService.hentSøker(personIdent)
        val geografiskTilknytning = utledGeografiskTilknytningKode(personService.hentGeografiskTilknytning(personIdent))
        val diskresjonskode =
            personinfo.adressebeskyttelse
                .singleOrNull()
                ?.gradering
                ?.tilDiskresjonskode()

        return ArbeidsfordelingKriterie(
            tema = arbeidsfordelingstema.name,
            diskresjonskode = diskresjonskode,
            geografiskOmraade = geografiskTilknytning ?: GEOGRAFISK_TILKNYTTING_OSLO,
            skjermet = egenAnsattService.erEgenAnsatt(personIdent),
            oppgavetype = oppgavetype,
        )
    }

    private fun utledGeografiskTilknytningKode(geografiskTilknytning: GeografiskTilknytningDto?): String? =
        geografiskTilknytning?.let {
            when (it.gtType) {
                GeografiskTilknytningType.BYDEL -> it.gtBydel
                GeografiskTilknytningType.KOMMUNE -> it.gtKommune
                GeografiskTilknytningType.UTLAND,
                GeografiskTilknytningType.UDEFINERT,
                -> {
                    secureLogger.info("Geografisk tilknytting=${it.gtType}")
                    null
                }
            }
        }
}
