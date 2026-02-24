package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.sikkerhet.EksternBrukerUtils
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(
    path = ["/api/ekstern/privat-bil"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class DagligReisePrivatBilController(
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val kjørelisteService: KjørelisteService,
    private val fagsakService: FagsakService,
) {
    @GetMapping("/rammevedtak")
    @ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"])
    fun hentRammevedtak(): List<RammevedtakDto> {
        val ident = EksternBrukerUtils.hentFnrFraToken()

        val kjørelister =
            fagsakService
                .finnFagsaker(setOf(ident))
                .flatMap { kjørelisteService.hentForFagsakId(it.id) }

        val rammevedtak = dagligReisePrivatBilService.hentRammevedtaksPrivatBil(ident)
        val kjøreliste = kjørelister.associateBy { it.data.reiseId }

        return rammevedtak.flatMap { it.tilDto(kjøreliste) }
    }
}

private fun RammevedtakPrivatBil.tilDto(kjøreliste: Map<ReiseId, Kjøreliste>): List<RammevedtakDto> =
    reiser.map { reise ->
        val kjøreliste = kjøreliste[reise.reiseId]
        RammevedtakDto(
            reiseId = reise.reiseId,
            fom = reise.grunnlag.fom,
            tom = reise.grunnlag.tom,
            reisedagerPerUke = reise.grunnlag.reisedagerPerUke,
            aktivitetsadresse = reise.aktivitetsadresse ?: "Ukjent adresse",
            aktivitetsnavn = "Ukjent aktivitet",
            uker = reise.grunnlag.alleDatoerGruppertPåUke()
                .map { (uke, datoer) ->
                    RammevedtakUkeDto(
                        fom = datoer.min(),
                        tom = datoer.max(),
                        ukeNummer = uke.ukenummer,
                        innsendtDato = kjøreliste?.datoMottatt?.toLocalDate(),
                        kanSendeInnKjøreliste =
                            uke.ukenummer <= LocalDate.now().ukenummer() && uke.år <= LocalDate.now().year,
                    )
                },
        )
    }
