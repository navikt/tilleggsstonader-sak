package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class Sporbar(
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
    @LastModifiedBy
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val endret: Endret = Endret(),
)

data class Endret(
    val endretAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    val endretTid: LocalDateTime = SporbarUtils.now(),
)

object SporbarUtils {
    fun now(): LocalDateTime = osloNow().truncatedTo(ChronoUnit.MILLIS)
}
