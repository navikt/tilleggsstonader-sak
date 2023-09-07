package no.nav.tilleggsstonader.sak.behandling.barn

import org.springframework.stereotype.Service

@Service
class BarnService

/**
 * Her skal vi opprette barn
 * Gjelder for [BARNETILSYN]
 *  * Skal opprette barn som kommer inn fra søknaden
 *  * Burde kunne opprette barn som blir lagt in manuellt via journalføringen?
 *  * Oppdatere personopplysninger -> oppdatere barn
 *  * Kopiere barn ved revurdering
 */
