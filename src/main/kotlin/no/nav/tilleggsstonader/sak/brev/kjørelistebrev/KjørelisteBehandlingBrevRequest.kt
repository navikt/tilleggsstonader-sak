package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.tilleggsstonader.sak.vedtak.dagligReise.PrivatBilOppsummertBeregningDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.SatsDagligReisePrivatBil
import java.time.LocalDate

data class KjørelisteBehandlingBrevRequest(
    val navn: String,
    val ident: String,
    val behandletDato: LocalDate,
    val behandlendeEnhet: String,
    val saksbehandlerSignatur: String? = null,
    val beregning: PrivatBilOppsummertBeregningDto,
    val satser: List<SatsDagligReisePrivatBil>,
)
