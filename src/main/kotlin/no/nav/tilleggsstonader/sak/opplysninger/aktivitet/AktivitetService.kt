package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
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
            fom = LocalDate.now().minusYears(3),
            tom = LocalDate.now().plusYears(1),
        )
    }
}
