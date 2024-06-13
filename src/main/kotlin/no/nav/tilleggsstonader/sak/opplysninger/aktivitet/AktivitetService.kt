package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.aktivitet.GruppeAktivitet
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class AktivitetService(
    private val fagsakPersonService: FagsakPersonService,
    private val aktivitetClient: AktivitetClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentAktiviteter(fagsakPersonId: UUID): List<AktivitetArenaDto> {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return aktivitetClient.hentAktiviteter(
            ident = ident,
            fom = osloDateNow().minusYears(3),
            tom = osloDateNow().plusYears(1),
        )
            .filter {
                try {
                    // Det er alltid gruppe=TILTAK når erStønadsberettiget = true (men alle av tiltak er ikke stønadsberettiget)
                    it.erStønadsberettiget == true || TypeAktivitet.valueOf(it.type).gruppe == GruppeAktivitet.TLTAK
                } catch (e: Exception) {
                    logger.error("TypeAktivitet mangler mapping, se secure logs for detaljer.")
                    secureLogger.error("TypeAktivitet=${it.type} mangler mapping. Vennligst oppdater TypeAktivitet med ny type.")
                    false
                }
            }
            .sortedByDescending { it.fom }
    }

    fun hentAktiviteterForGrunnlagsdata(ident: String, fom: LocalDate, tom: LocalDate): List<AktivitetArenaDto> {
        return aktivitetClient.hentAktiviteter(
            ident = ident,
            fom = fom,
            tom = tom,
        )
            // Det er alltid gruppe=TILTAK når erStønadsberettiget = true (men alle av tiltak er ikke stønadsberettiget)
            .filter { it.erStønadsberettiget == true }
            .sortedByDescending { it.fom }
    }
}
