package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.config.SecureLogger
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory

object StegValidering {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun validerHarTilgang(
        rolleConfig: RolleConfig,
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
    ) {
        val rolleForSteg = saksbehandling.steg.tillattFor
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, rolleForSteg)

        logger.info("Starter håndtering av $stegType på behandling ${saksbehandling.id}")
        SecureLogger.secureLogger.info(
            "Starter håndtering av $stegType på behandling " +
                "${saksbehandling.id} med saksbehandler=[$saksbehandlerIdent]",
        )

        feilHvis(!harTilgangTilSteg) {
            "$saksbehandlerIdent kan ikke utføre steg '${stegType.displayName()}' pga manglende rolle."
        }
    }

    fun validerGyldigTilstand(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
    ) {
        if (saksbehandling.steg == StegType.BEHANDLING_FERDIGSTILT) {
            error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
        }

        if (stegType.kommerEtter(saksbehandling.steg)) {
            error(
                "$saksbehandlerIdent prøver å utføre steg '${stegType.displayName()}', " +
                    "men behandlingen er på steg '${saksbehandling.steg.displayName()}'",
            )
        }

        if (saksbehandling.steg == StegType.BESLUTTE_VEDTAK && stegType != StegType.BESLUTTE_VEDTAK) {
            error("Behandlingen er på steg '${saksbehandling.steg.displayName()}', og er da låst for alle andre type endringer.")
        }
    }

    fun validerAtStegKanResettes(
        rolleConfig: RolleConfig,
        behandling: Behandling,
        steg: StegType,
    ) {
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)
        val harTilgangTilNesteSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, steg.tillattFor)
        if (!harTilgangTilSteg || !harTilgangTilNesteSteg) {
            val saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
            error(
                "$saksbehandler kan ikke endre" +
                    " fra steg=${behandling.steg.displayName()} til steg=${steg.displayName()}" +
                    " pga manglende rolle på behandling=$behandling.id",
            )
        }
    }
}
