package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
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
    fun hentRammevedtaksPrivatBil(ident: String): List<RammevedtakPrivatBil> =
        hentRammevedtakPåIdent(ident)
            .mapNotNull { it.data.rammevedtakPrivatBil }

    fun hentRammevedtakPåIdent(ident: String): List<GeneriskVedtak<InnvilgelseEllerOpphørDagligReise>> {
        val alleIdenterPåPerson = hentAlleIdenterPåPerson(ident)
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

    // For å kunne støtte kall fra soknad-api i sluttbruker sin context
    private fun hentAlleIdenterPåPerson(ident: String): Set<String> {
        val pdlIdenter =
            if (SikkerhetContext.erTokenMedIssuerTokenX()) {
                personService.hentFolkeregisterIdenterISluttbrukerContext(ident)
            } else {
                personService.hentFolkeregisterIdenter(ident)
            }

        return pdlIdenter.identer
            .map { it.ident }
            .toSet()
    }

    fun hentRammevedtakForBehandlingId(behandlingId: BehandlingId): RammevedtakPrivatBil? =
        vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandlingId)?.data?.rammevedtakPrivatBil
}
