package no.nav.tilleggsstonader.sak.tilgang

import no.nav.tilleggsstonader.libs.spring.cache.getValue
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ManglerTilgang
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.hentGrupperFraToken
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

/**
 * Det blir kanskje litt mye cache med cache her og i [TilgangskontrollService]
 */
@Service
class TilgangService(
    private val tilgangskontrollService: TilgangskontrollService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
    private val rolleConfig: RolleConfig,
    private val cacheManager: CacheManager,
    private val auditLogger: AuditLogger,
) {
    /**
     * Kun ved tilgangskontroll for enkeltperson (eks når man skal søke etter brevmottaker)
     * Ellers bruk [validerTilgangTilPersonMedRelasjoner]
     */
    fun validerTilgangTilPerson(
        personIdent: String,
        event: AuditLoggerEvent,
    ) {
        val tilgang = tilgangskontrollService.sjekkTilgang(personIdent, SikkerhetContext.hentToken())
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        kastFeilHvisIkkeTilgang(tilgang, "person", personIdent)
    }

    fun validerTilgangTilPersonMedRelasjoner(
        personIdent: String,
        event: AuditLoggerEvent,
    ) {
        val tilgang =
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(personIdent, SikkerhetContext.hentToken())
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        kastFeilHvisIkkeTilgang(tilgang, "person", personIdent)
    }

    /**
     * Cachear henting av saksehandling då vi kun skal bruke ident og fagsakPersonId
     */
    fun validerTilgangTilBehandling(
        behandlingId: BehandlingId,
        event: AuditLoggerEvent,
    ) {
        val saksbehandling =
            cacheManager.getValue("tilgangService_behandling", behandlingId) {
                behandlingService.hentSaksbehandling(behandlingId)
            }
        val tilgang =
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(
                personIdent = saksbehandling.ident,
                jwtToken = SikkerhetContext.hentToken(),
            )
        val key = CustomKeyValue("behandling", behandlingId.id)
        auditLogger.log(Sporingsdata(event, saksbehandling.ident, tilgang, custom1 = key))
        kastFeilHvisIkkeTilgang(tilgang, "behandling", behandlingId.id.toString())
    }

    /**
     * Cachear henting av fagsak då vi kun skal bruke ident og fagsakPersonId
     */
    fun validerTilgangTilFagsak(
        fagsakId: FagsakId,
        event: AuditLoggerEvent,
    ) {
        val fagsak =
            cacheManager.getValue("tilgangService_fagsakIdent", fagsakId) {
                fagsakService.hentFagsak(fagsakId)
            }
        validerTilgangTilFagsak(fagsak, event)
    }

    /**
     * Cachear henting av fagsak då vi kun skal bruke ident og fagsakPersonId
     */
    fun validerTilgangTilEksternFagsak(
        eksternFagsakId: Long,
        event: AuditLoggerEvent,
    ) {
        val fagsak =
            cacheManager.getValue("tilgangService_eksternFagsakId", eksternFagsakId) {
                fagsakService.hentFagsakPåEksternId(eksternFagsakId = eksternFagsakId)
            }
        validerTilgangTilFagsak(fagsak, event)
    }

    private fun validerTilgangTilFagsak(
        fagsak: Fagsak,
        event: AuditLoggerEvent,
    ) {
        val fagsakId = fagsak.id
        val tilgang =
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(
                personIdent = fagsak.hentAktivIdent(),
                SikkerhetContext.hentToken(),
            )
        val custom1 = CustomKeyValue("fagsak", fagsakId.id)
        auditLogger.log(Sporingsdata(event, fagsak.hentAktivIdent(), tilgang, custom1 = custom1))
        kastFeilHvisIkkeTilgang(tilgang, "fagsak", fagsakId.toString())
    }

    fun validerTilgangTilFagsakPerson(
        fagsakPersonId: FagsakPersonId,
        event: AuditLoggerEvent,
    ) {
        val personIdent =
            cacheManager.getValue("fagsakPersonIdent", fagsakPersonId) {
                fagsakPersonService.hentAktivIdent(fagsakPersonId)
            }
        val tilgang =
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(
                personIdent = personIdent,
                jwtToken = SikkerhetContext.hentToken(),
            )
        val custom1 = CustomKeyValue("fagsakPersonId", fagsakPersonId.id)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang, custom1 = custom1))
        kastFeilHvisIkkeTilgang(tilgang, "fagsakPerson", fagsakPersonId.toString())
    }

    private fun kastFeilHvisIkkeTilgang(
        tilgang: Tilgang,
        type: String,
        typeId: String,
    ) {
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                        "har ikke tilgang til $type=$typeId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.begrunnelse}",
            )
        }
    }

    fun validerHarSaksbehandlerrolle() {
        validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
    }

    fun validerHarBeslutterrolle() {
        validerTilgangTilRolle(BehandlerRolle.BESLUTTER)
    }

    fun validerTilgangTilRolle(minimumsrolle: BehandlerRolle) {
        if (!harTilgangTilRolle(minimumsrolle)) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} har ikke tilgang " +
                        "til å utføre denne operasjonen som krever minimumsrolle $minimumsrolle",
                frontendFeilmelding = "Mangler nødvendig saksbehandlerrolle for å utføre handlingen",
            )
        }
    }

    fun harTilgangTilRolle(minimumsrolle: BehandlerRolle): Boolean = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, minimumsrolle)

    fun harEgenAnsattRolle(): Boolean = hentGrupperFraToken().contains(rolleConfig.egenAnsatt)

    fun harStrengtFortroligRolle(): Boolean = hentGrupperFraToken().contains(rolleConfig.kode6)

    /**
     * Filtrerer data basert på om man har tilgang til den eller ikke
     * Filtrer ikke på egen ansatt
     */
    fun <T> filtrerUtFortroligDataForRolle(
        values: List<T>,
        fn: (T) -> Adressebeskyttelse?,
    ): List<T> {
        val grupper = hentGrupperFraToken()
        val kode6gruppe = grupper.contains(rolleConfig.kode6)
        val kode7Gruppe = grupper.contains(rolleConfig.kode7)
        return values.filter {
            when (fn(it)?.gradering) {
                FORTROLIG -> kode7Gruppe
                STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND -> kode6gruppe
                else -> (!kode6gruppe)
            }
        }
    }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(
        cacheName: String,
        verdi: T,
        hentVerdi: () -> Tilgang,
    ): Tilgang {
        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler())) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }

    fun validerSaksbehandler(saksbehandler: String): Boolean = SikkerhetContext.hentSaksbehandlerEllerSystembruker() == saksbehandler
}
