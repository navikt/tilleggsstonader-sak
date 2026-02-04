package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service

@Service
class DagligReisePrivatBilService(
    private val fagsakPersonService: FagsakPersonService,
    private val personService: PersonService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakService: VedtakService,
) {
    fun hentRammevedtaksPrivatBil(ident: IdentRequest): List<RammevedtakDto> =
        hentRammevedtakPåBruker(ident).flatMap {
            mapRammevedtakTilDto(it)
        }

    private fun hentRammevedtakPåBruker(ident: IdentRequest): List<RammevedtakPrivatBil> {
        val alleIdenterPåPerson =
            personService
                .hentFolkeregisterIdenter(ident.ident)
                .identer
                .map { it.ident }
                .toSet()
        val fagsakPerson = fagsakPersonService.finnPerson(alleIdenterPåPerson)

        brukerfeilHvis(fagsakPerson == null) {
            "Fant ingen fagsakperson for ident"
        }

        val iverksatteBehandlingIder =
            behandlingRepository.finnSisteIverksatteBehandlingerForFagsakPersonId(
                fagsakPersonId = fagsakPerson.id,
                stønadstyper = listOf(Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR),
            )

        return iverksatteBehandlingIder.mapNotNull {
            vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(it)?.data?.rammevedtakPrivatBil
        }
    }
}

private fun mapRammevedtakTilDto(rammevedtak: RammevedtakPrivatBil): List<RammevedtakDto> =
    rammevedtak.reiser.map { reise ->
        RammevedtakDto(
            id = reise.reiseId,
            fom = reise.grunnlag.fom,
            tom = reise.grunnlag.tom,
            reisedagerPerUke = reise.grunnlag.reisedagerPerUke,
            aktivitetsadresse = reise.aktivitetsadresse ?: "Ukjent adresse",
            aktivitetsnavn = "Ukjent aktivitet",
            uker =
                reise.uker.mapIndexed { idx, uke ->
                    RammevedtakUkeDto(
                        fom = uke.grunnlag.fom,
                        tom = uke.grunnlag.tom,
                        ukeNummer = idx + 1,
                    )
                },
        )
    }
