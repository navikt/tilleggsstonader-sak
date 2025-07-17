package no.nav.tilleggsstonader.sak.klage

import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.klage.dto.KlagebehandlingerDto
import no.nav.tilleggsstonader.sak.klage.dto.OpprettKlageDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class KlageService(
    private val fagsakService: FagsakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val klageClient: KlageClient,
) {
    fun hentBehandlinger(fagsakPersonId: FagsakPersonId): KlagebehandlingerDto {
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)
        val eksterneFagsakIder = listOfNotNull(fagsaker.barnetilsyn, fagsaker.læremidler, fagsaker.boutgifter).map { it.eksternId.id }

        if (eksterneFagsakIder.isEmpty()) {
            return KlagebehandlingerDto.empty()
        }

        val klagebehandlingerPåEksternId =
            klageClient.hentKlagebehandlinger(eksterneFagsakIder.toSet()).mapValues { klagebehandlingerPåEksternId ->
                klagebehandlingerPåEksternId.value.map { brukVedtaksdatoFraKlageinstansHvisOversendt(it) }
            }

        return KlagebehandlingerDto(
            tilsynBarn = klagebehandlingerPåEksternId[fagsaker.barnetilsyn?.eksternId?.id] ?: emptyList(),
            læremidler = klagebehandlingerPåEksternId[fagsaker.læremidler?.eksternId?.id] ?: emptyList(),
            boutgifter = klagebehandlingerPåEksternId[fagsaker.boutgifter?.eksternId?.id] ?: emptyList(),
            dagligReiseTSO = klagebehandlingerPåEksternId[fagsaker.dagligReiseTSO?.eksternId?.id] ?: emptyList(),
            dagligReiseTSR = klagebehandlingerPåEksternId[fagsaker.dagligReiseTSR?.eksternId?.id] ?: emptyList(),
        )
    }

    fun opprettKlage(
        fagsakId: FagsakId,
        opprettKlageDto: OpprettKlageDto,
    ) {
        val klageMottatt = opprettKlageDto.mottattDato

        brukerfeilHvis(klageMottatt.isAfter(LocalDate.now())) {
            "Kan ikke opprette klage med krav mottatt frem i tid for fagsak=$fagsakId"
        }

        opprettKlage(fagsakService.hentFagsak(fagsakId), opprettKlageDto.mottattDato)
    }

    fun hentBehandlingIderForOppgaveIder(oppgaveIder: List<Long>): Map<Long, UUID> = klageClient.hentBehandlingerForOppgaveIder(oppgaveIder)

    private fun opprettKlage(
        fagsak: Fagsak,
        klageMottatt: LocalDate,
    ) {
        val aktivIdent = fagsak.hentAktivIdent()
        val enhetId = arbeidsfordelingService.hentNavEnhet(aktivIdent, fagsak.stønadstype)?.enhetNr
        brukerfeilHvis(enhetId == null) {
            "Finner ikke behandlende enhet for personen"
        }
        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = aktivIdent,
                stønadstype = fagsak.stønadstype,
                eksternFagsakId = fagsak.eksternId.id.toString(),
                fagsystem = Fagsystem.TILLEGGSSTONADER,
                klageMottatt = klageMottatt,
                behandlendeEnhet = enhetId,
            ),
        )
    }

    private fun brukVedtaksdatoFraKlageinstansHvisOversendt(klagebehandling: KlagebehandlingDto): KlagebehandlingDto {
        val erOversendtTilKlageinstans = klagebehandling.resultat == BehandlingResultat.IKKE_MEDHOLD
        val vedtaksdato =
            if (erOversendtTilKlageinstans) {
                klagebehandling.klageinstansResultat
                    .singleOrNull { klageinnstansResultat ->
                        klageinnstansResultat.type ==
                            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ||
                            klageinnstansResultat.type == BehandlingEventType.BEHANDLING_FEILREGISTRERT
                    }?.mottattEllerAvsluttetTidspunkt
            } else {
                klagebehandling.vedtaksdato
            }
        return klagebehandling.copy(vedtaksdato = vedtaksdato)
    }
}
