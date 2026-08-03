package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.ekstern.stønad.finnForUke
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.util.erFørNåværendeUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KjørelisteManuellRegistreringService(
    private val kjørelisteService: KjørelisteService,
    private val behandlingService: BehandlingService,
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
) {
    fun hentKjørelisteOversikt(behandlingId: BehandlingId): KjørelisteOversiktDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val kjørelister = kjørelisteService.hentForFagsakId(behandling.fagsakId)

        val reiserIRammevedtak =
            dagligReisePrivatBilService.hentRammevedtakForBehandlingId(behandlingId)?.reiser
                ?: error("Forventer at det finnes reiser i rammevedtak ...")

        val tilgjengeligeReiser = finnKjørelisterSomKanFyllesUt(kjørelister, reiserIRammevedtak)
        val kjørelisterLagretIDenneBehandlingen =
            finnKjørelisterInnsendtIDenneBehandlingen(
                behandlingId = behandlingId,
                kjørelister = kjørelister,
                reiserIRammevedtak = reiserIRammevedtak,
            )

        return KjørelisteOversiktDto(
            tilgjengeligeReiser = tilgjengeligeReiser,
            kjørelisterLagretIBehandling = kjørelisterLagretIDenneBehandlingen,
        )
    }

    fun lagreManuellKjøreliste(
        behandlingId: BehandlingId,
        request: LagreManuellKjørelisteRequest,
    ): KjørelisteId {
        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        val innsendtKjøreliste = InnsendtKjøreliste(reiseId = request.reiseId, reisedager = request.reisedager)

        validerInnsendtKjøreliste(behandling = behandling, innsendtKjøreliste = innsendtKjøreliste)

        val kjøreliste =
            kjørelisteService.lagre(
                innsendtKjøreliste = innsendtKjøreliste,
                fagsakId = behandling.fagsakId,
                journalpostId = request.journalpostId,
                begrunnelse = request.begrunnelse,
                behandlingId = behandlingId,
                manueltRegistrert = true,
            )
        return kjøreliste.id
    }

    private fun validerInnsendtKjøreliste(
        behandling: Behandling,
        innsendtKjøreliste: InnsendtKjøreliste,
    ) {
        val rammeForReise =
            dagligReisePrivatBilService.hentRammevedtakForReiseIBehandling(behandling.id, innsendtKjøreliste.reiseId)

        feilHvis(rammeForReise == null) {
            "Fant ikke rammevedtak for reise ${innsendtKjøreliste.reiseId}"
        }

        brukerfeilHvis(!rammeForReise.grunnlag.inneholder(innsendtKjøreliste)) {
            "Innsendte dager er ikke innenfor perioden i rammevedtaket"
        }

        brukerfeilHvis(innsendtKjøreliste.reisedager.any { it.dato > LocalDate.now() }) {
            "Kan ikke registrere kjøreliste for dager som er fremover i tid"
        }

        validerDagerIkkeTidligereInnsendt(
            fagsakId = behandling.fagsakId,
            reiseId = innsendtKjøreliste.reiseId,
            innsendtKjøreliste = innsendtKjøreliste,
        )
    }

    private fun validerDagerIkkeTidligereInnsendt(
        fagsakId: FagsakId,
        reiseId: ReiseId,
        innsendtKjøreliste: InnsendtKjøreliste,
    ) {
        val eksisterendeKjørelister =
            kjørelisteService
                .hentForFagsakId(fagsakId)
                .filter { it.data.reiseId == reiseId }

        val tidligereInnsendteDager =
            eksisterendeKjørelister.flatMap { it.data.reisedager.map { dag -> dag.dato } }.toSet()

        val overlappendeDatoer = innsendtKjøreliste.reisedager.map { it.dato }.filter { it in tidligereInnsendteDager }
        brukerfeilHvis(overlappendeDatoer.isNotEmpty()) {
            "Følgende datoer er allerede registrert i en tidligere kjøreliste: ${overlappendeDatoer.joinToString()}"
        }
    }

    fun avklarKjørelisterRegistrertIBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        val kjørelisterRegistrertIBehandling = kjørelisteService.hentManueltLagredeIBehandling(saksbehandling.id)
        val avklarteKjørelisteIds =
            avklartKjørelisteService
                .hentAvklarteUkerForBehandling(behandlingId)
                .map { it.kjørelisteId }
                .toSet()

        kjørelisterRegistrertIBehandling
            .filter { it.id !in avklarteKjørelisteIds }
            .forEach { avklartKjørelisteService.avklarUkerFraKjøreliste(behandlingId, it) }
    }

    private fun finnKjørelisterInnsendtIDenneBehandlingen(
        behandlingId: BehandlingId,
        kjørelister: List<Kjøreliste>,
        reiserIRammevedtak: List<RammevedtakForReiseMedPrivatBil>,
    ): List<ManueltInnsendtKjørelisteDto> {
        val kjørelisterRegistrertIDenneBehandlingen = kjørelister.filter { it.behandlingId == behandlingId }

        return kjørelisterRegistrertIDenneBehandlingen.map { kjøreliste ->
            val tilhørendeReise = reiserIRammevedtak.single { it.reiseId == kjøreliste.data.reiseId }

            ManueltInnsendtKjørelisteDto(
                id = kjøreliste.id,
                journalpostId = kjøreliste.journalpostId,
                reiseFom = tilhørendeReise.grunnlag.fom,
                reiseTom = tilhørendeReise.grunnlag.tom,
                aktivitetsadresse = tilhørendeReise.aktivitetsadresse,
                begrunnelse = kjøreliste.begrunnelse,
                innsendteUker = kjøreliste.data.reisedager.tilKjørelisteUker(),
            )
        }
    }

    private fun finnKjørelisterSomKanFyllesUt(
        kjørelister: List<Kjøreliste>,
        reiserIRammevedtak: List<RammevedtakForReiseMedPrivatBil>,
    ): List<ManuellRegistreringReiseDto> =
        reiserIRammevedtak.map { reise ->
            val kjørelisterForReise = kjørelister.filter { it.data.reiseId == reise.reiseId }

            ManuellRegistreringReiseDto(
                reiseId = reise.reiseId,
                aktivitetsadresse = reise.aktivitetsadresse,
                fom = reise.grunnlag.fom,
                tom = reise.grunnlag.tom,
                uker =
                    reise.grunnlag
                        .alleDatoerGruppertPåUke()
                        .filter { (uke, _) -> uke.erFørNåværendeUke() }
                        .map { (uke, datoer) ->
                            val kjørelisteForUke = kjørelisterForReise.finnForUke(uke)

                            ManuellRegistreringUkeDto(
                                ukenummer = uke.ukenummer,
                                fom = datoer.min(),
                                tom = datoer.max(),
                                innsendtTidligere = kjørelisteForUke != null,
                                dager = datoer,
                            )
                        },
            )
        }

    private fun List<KjørelisteDag>.tilKjørelisteUker(): List<ManueltInnsendtKjørelisteUkeDto> =
        this
            .groupBy { it.dato.tilUkeIÅr() }
            .entries
            .sortedBy { it.key }
            .map { (uke, dager) ->
                ManueltInnsendtKjørelisteUkeDto(
                    ukenummer = uke.ukenummer,
                    fom = dager.minOf { it.dato },
                    tom = dager.maxOf { it.dato },
                    dager = dager.sortedBy { it.dato },
                )
            }
}
