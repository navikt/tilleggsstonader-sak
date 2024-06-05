package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class AktivitetService(
    private val fagsakPersonService: FagsakPersonService,
    private val aktivitetClient: AktivitetClient,
) {
    fun hentAktiviteter(fagsakPersonId: UUID): List<AktivitetArenaDto> {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return aktivitetClient.hentAktiviteter(
            ident = ident,
            fom = osloDateNow().minusYears(3),
            tom = osloDateNow().plusYears(1),
        ).sortedByDescending { it.fom }
    }

    fun hentAktiviteterForGrunnlagsdata(ident: String, fom: LocalDate, tom: LocalDate): List<AktivitetArenaDto> {
        return aktivitetClient.hentAktiviteter(
            ident = ident,
            fom = fom,
            tom = tom,
        ).sortedByDescending { it.fom }
    }
}
