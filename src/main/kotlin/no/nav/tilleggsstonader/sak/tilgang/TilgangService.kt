package no.nav.tilleggsstonader.sak.tilgang

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.infrastruktur.config.getValue
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
import java.util.UUID

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
     * Kun ved tilgangskontroll for enskild person, ellers bruk [validerTilgangTilPersonMedBarn]
     */
    fun validerTilgangTilPerson(personIdent: String, event: AuditLoggerEvent) {
        val tilgang = tilgangskontrollService.sjekkTilgang(personIdent, SikkerhetContext.hentToken())
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        if (!tilgang.harTilgang) {
            secureLogger.warn(
                "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til $personIdent",
            )
            throw ManglerTilgang(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til person",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.begrunnelse}",
            )
        }
    }

    fun validerTilgangTilPersonMedBarn(personIdent: String, event: AuditLoggerEvent) {
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        if (!tilgang.harTilgang) {
            secureLogger.warn(
                "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til $personIdent eller dets barn",
            )
            throw ManglerTilgang(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til person eller dets barn",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.begrunnelse}",
            )
        }
    }

    fun validerTilgangTilBehandling(behandlingId: UUID, event: AuditLoggerEvent) {
        val personIdent = cacheManager.getValue("behandlingPersonIdent", behandlingId) {
            behandlingService.hentAktivIdent(behandlingId)
        }
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(
            Sporingsdata(event, personIdent, tilgang, custom1 = CustomKeyValue("behandling", behandlingId)),
        )
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til behandling=$behandlingId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.begrunnelse}",
            )
        }
    }

    fun validerTilgangTilBehandling(saksbehandling: Saksbehandling, event: AuditLoggerEvent) {
        val tilgang = harTilgangTilPersonMedRelasjoner(saksbehandling.ident)
        auditLogger.log(
            Sporingsdata(event, saksbehandling.ident, tilgang, CustomKeyValue("behandling", saksbehandling.id)),
        )
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til behandling=${saksbehandling.id}",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.begrunnelse}",
            )
        }
    }

    fun validerTilgangTilFagsak(fagsakId: UUID, event: AuditLoggerEvent) {
        val personIdent = cacheManager.getValue("fagsakIdent", fagsakId) {
            fagsakService.hentAktivIdent(fagsakId)
        }
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang, custom1 = CustomKeyValue("fagsak", fagsakId)))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til fagsak=$fagsakId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.begrunnelse}",
            )
        }
    }

    fun validerTilgangTilEksternFagsak(eksternFagsakId: Long, event: AuditLoggerEvent) {
        val fagsakId = cacheManager.getValue("eksternFagsakId", eksternFagsakId) {
            fagsakService.hentFagsakDtoPåEksternId(eksternFagsakId = eksternFagsakId).id
        }
        validerTilgangTilFagsak(fagsakId, event)
    }

    fun validerTilgangTilFagsakPerson(fagsakPersonId: UUID, event: AuditLoggerEvent) {
        val personIdent = cacheManager.getValue("fagsakPersonIdent", fagsakPersonId) {
            fagsakPersonService.hentAktivIdent(fagsakPersonId)
        }
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(
            Sporingsdata(event, personIdent, tilgang, custom1 = CustomKeyValue("fagsakPersonId", fagsakPersonId)),
        )
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                    "har ikke tilgang til fagsakPerson=$fagsakPersonId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.begrunnelse}",
            )
        }
    }

    private fun harTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        return harSaksbehandlerTilgang("validerTilgangTilPersonMedBarn", personIdent) {
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(personIdent, SikkerhetContext.hentToken())
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
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} har ikke tilgang " +
                    "til å utføre denne operasjonen som krever minimumsrolle $minimumsrolle",
                frontendFeilmelding = "Mangler nødvendig saksbehandlerrolle for å utføre handlingen",
            )
        }
    }

    fun harTilgangTilRolle(minimumsrolle: BehandlerRolle): Boolean {
        return SikkerhetContext.harTilgangTilGittRolle(rolleConfig, minimumsrolle)
    }

    fun harEgenAnsattRolle(): Boolean =
        hentGrupperFraToken().contains(rolleConfig.egenAnsatt)

    /**
     * Filtrerer data basert på om man har tilgang til den eller ikke
     * Filtrer ikke på egen ansatt
     */
    fun <T> filtrerUtFortroligDataForRolle(values: List<T>, fn: (T) -> Adressebeskyttelse?): List<T> {
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
    private fun <T> harSaksbehandlerTilgang(cacheName: String, verdi: T, hentVerdi: () -> Tilgang): Tilgang {
        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler())) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }

    fun validerSaksbehandler(saksbehandler: String): Boolean {
        return SikkerhetContext.hentSaksbehandlerEllerSystembruker() == saksbehandler
    }
}
