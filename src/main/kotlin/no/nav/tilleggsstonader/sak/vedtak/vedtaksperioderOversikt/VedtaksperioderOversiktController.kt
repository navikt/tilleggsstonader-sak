package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
class VedtaksperioderOversiktController(
    private val vedtakOversiktService: VedtaksperioderOversiktService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/fullstendig-oversikt/{fagsakPersonId}")
    fun hentFullstendigVedtaksoversikt(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): VedtaksperiodeOversiktDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return tilDto(vedtakOversiktService.hentVedtakOversikt(fagsakPersonId))
    }
}

private fun tilDto(vedtaksperioder: VedtaksperioderOversikt): VedtaksperiodeOversiktDto =
    VedtaksperiodeOversiktDto(
        tilsynBarn =
            vedtaksperioder.tilsynBarn.map { it ->
                VedtaksperiodeOversiktTilsynBarnDto(
                    fom = it.fom,
                    tom = it.tom,
                    aktivitet = it.aktivitet,
                    målgruppe = it.målgruppe,
                    antallBarn = it.antallBarn,
                    totalMånedsUtgift = it.totalMånedsUtgift,
                )
            },
        læremidler =
            vedtaksperioder.læremidler.map { it ->
                VedtaksperiodeOversiktLæremidlerDto(
                    fom = it.fom,
                    tom = it.tom,
                    aktivitet = it.aktivitet,
                    målgruppe = it.målgruppe,
                    antallMåneder = it.antallMåneder,
                    studienivå = it.studienivå,
                    studieprosent = it.studieprosent,
                    månedsbeløp = it.månedsbeløp,
                )
            },
    )
