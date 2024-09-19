package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.aktivitet.GruppeAktivitet
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AktivitetService(
    private val fagsakPersonService: FagsakPersonService,
    private val aktivitetClient: AktivitetClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentAktiviteterMedPerioder(
        fagsakPersonId: FagsakPersonId,
        fom: LocalDate = osloDateNow().minusYears(3),
        tom: LocalDate = osloDateNow().plusYears(1),
    ): AktiviteterDto = AktiviteterDto(
        periodeHentetFra = fom,
        periodeHentetTil = tom,
        aktiviteter = hentAktiviteter(fagsakPersonId, fom, tom),
    )

    fun hentAktiviteter(
        fagsakPersonId: FagsakPersonId,
        fom: LocalDate = osloDateNow().minusYears(3),
        tom: LocalDate = osloDateNow().plusYears(1),
    ): List<AktivitetArenaDto> {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return hentStønadsberettigedeTiltak(ident, fom, tom)
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

    private fun hentStønadsberettigedeTiltak(
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
    ) = aktivitetClient.hentAktiviteter(
        ident = ident,
        fom = fom,
        tom = tom,
    )
        .filter {
            try {
                // Ikke alle aktiviteter har fått flagg "stønadsberettiget" i Arena selv om de skulle hatt det, så vi trenger en ekstra sjekk på gruppe
                // Det er alltid gruppe=TILTAK når erStønadsberettiget = true, men ikke alle tiltak er stønadsberettiget
                it.erStønadsberettiget == true || TypeAktivitet.valueOf(it.type).gruppe == GruppeAktivitet.TLTAK
            } catch (e: Exception) {
                logger.error("TypeAktivitet mangler mapping, se secure logs for detaljer.")
                secureLogger.error("TypeAktivitet=${it.type} mangler mapping. Vennligst oppdater TypeAktivitet med ny type.")
                false
            }
        }
        .sortedByDescending { it.fom }
}
