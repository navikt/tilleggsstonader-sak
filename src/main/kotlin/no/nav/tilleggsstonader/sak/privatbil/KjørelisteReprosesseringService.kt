package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class KjørelisteReprosesseringService(
    private val kjørelisteService: KjørelisteService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
    private val behandleMottattKjørelisteService: BehandleMottattKjørelisteService,
) {
    fun finnTilgjengeligeKjørelister(eksternFagsakId: Long): List<TilgjengeligKjøreliste> {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)
        val kjørelisteIderMedIkkeHenlagteBehandlinger =
            behandlingService
                .finnAlleBehandlingerForFagsak(fagsak.id)
                .filterNot { it.erHenlagt() }
                .flatMap { avklartKjørelisteService.hentAvklarteUkerForBehandling(it.id) }
                .map { it.kjørelisteId }
                .toSet()

        return kjørelisteService
            .hentForFagsakId(fagsak.id)
            .filter { it.manueltLagretIBehandling == null }
            .filterNot { it.id in kjørelisteIderMedIkkeHenlagteBehandlinger }
            .map { kjøreliste ->
                TilgjengeligKjøreliste(
                    kjørelisteId = kjøreliste.id,
                    reiseId = kjøreliste.data.reiseId,
                    datoMottatt = kjøreliste.datoMottatt.toLocalDate(),
                    uker =
                        kjøreliste.data.reisedager
                            .map { it.dato.tilUkeIÅr().toString() }
                            .distinct()
                            .sorted(),
                )
            }
    }

    @Transactional
    fun behandlePåNytt(kjørelisteId: KjørelisteId) {
        val kjøreliste = kjørelisteService.hentKjøreliste(kjørelisteId)
        logger.info("Behandler kjøreliste $kjørelisteId for fagsak ${kjøreliste.fagsakId} på nytt")

        brukerfeilHvis(kjøreliste.manueltLagretIBehandling != null) {
            "Kan ikke reprosessere en kjøreliste som er manuelt registrert"
        }

        val behandlinger = behandlingService.finnAlleBehandlingerForFagsak(kjøreliste.fagsakId)
        val alleredeBehandlet =
            behandlinger
                .filterNot { it.erHenlagt() }
                .flatMap { avklartKjørelisteService.hentAvklarteUkerForBehandling(it.id) }
                .any { it.kjørelisteId == kjøreliste.id }

        brukerfeilHvis(alleredeBehandlet) {
            "Kjørelisten er allerede knyttet til en behandling som ikke er henlagt"
        }

        behandleMottattKjørelisteService.behandleMottattKjøreliste(kjøreliste)
    }
}

data class TilgjengeligKjøreliste(
    val kjørelisteId: KjørelisteId,
    val reiseId: no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId,
    val datoMottatt: LocalDate,
    val uker: List<String>,
)
