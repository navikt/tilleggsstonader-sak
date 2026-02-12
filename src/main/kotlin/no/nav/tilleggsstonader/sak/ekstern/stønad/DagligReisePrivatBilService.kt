package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service

@Service
class DagligReisePrivatBilService(
    private val fagsakPersonService: FagsakPersonService,
    private val personService: PersonService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakService: VedtakService,
) {
    fun hentRammevedtaksPrivatBil(ident: IdentRequest): List<RammevedtakPrivatBil> =
        hentRammevedtakPåIdent(ident.ident)
            .mapNotNull { it.data.rammevedtakPrivatBil }

    fun hentRammevedtakPåIdent(ident: String): List<GeneriskVedtak<InnvilgelseEllerOpphørDagligReise>> {
        val alleIdenterPåPerson =
            personService
                .hentFolkeregisterIdenter(ident)
                .identer
                .map { it.ident }
                .toSet()
        val fagsakPerson = fagsakPersonService.finnPerson(alleIdenterPåPerson)

        val iverksatteBehandlingIder =
            behandlingRepository.finnSisteIverksatteBehandlingerForFagsakPersonId(
                fagsakPersonId = fagsakPerson?.id!!,
                stønadstyper = listOf(Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR),
            )

        return iverksatteBehandlingIder.mapNotNull {
            vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(it)
        }
    }
}
