package no.nav.tilleggsstonader.sak.util

import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.MaxAntallRekjøringerException
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.utils.osloNow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Avbryter opprinnelig task, og setter opp ny kjøring etter et spesifisert antall dager.
 *
 * Etter at maks antall tillatte forsøk har blitt brukt opp, blir tasken satt til feilet.
 *
 * @param årsak brukes til å telle antall rekjøringer, så det er fint om verdien ikke endrer seg
 * mellom hver kjøring.
 */
fun TaskService.rekjørTaskSenere(
    task: Task,
    årsak: String,
    antallDagerTilNesteRekjøring: Long = 7L,
    totaltAntallRekjøringerFørFeiling: Int = 26,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    val antallRekjøringer =
        findTaskLoggByTaskId(task.id)
            .count { it.type == Loggtype.KLAR_TIL_PLUKK && it.melding?.startsWith(årsak) == true }
    if (antallRekjøringer < totaltAntallRekjøringerFørFeiling) {
        logger.info(
            "Rekjøring nummer $antallRekjøringer/$totaltAntallRekjøringerFørFeiling av task. " +
                "Årsak til rekjøring: $årsak. " +
                "Prøver å kjøre task på nytt om $antallDagerTilNesteRekjøring dager",
        )
        throw RekjørSenereException(
            årsak = årsak,
            triggerTid = osloNow().plusDays(antallDagerTilNesteRekjøring),
        )
    } else {
        throw MaxAntallRekjøringerException(maxAntallRekjøring = totaltAntallRekjøringerFørFeiling)
    }
}
