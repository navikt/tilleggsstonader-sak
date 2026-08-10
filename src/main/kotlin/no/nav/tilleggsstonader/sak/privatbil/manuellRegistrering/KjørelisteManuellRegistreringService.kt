package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.ekstern.stønad.finnForUke
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.util.erFørNåværendeUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KjørelisteManuellRegistreringService(
    private val kjørelisteService: KjørelisteService,
    private val behandlingService: BehandlingService,
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
    private val kjørelisteJournalpostValidering: KjørelisteJournalpostValidering,
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
    ): Kjøreliste {
        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        val innsendtKjøreliste =
            InnsendtKjøreliste(
                reiseId = request.reiseId,
                reisedager = request.reisedager,
            )

        validerManuellKjøreliste(behandling = behandling, innsendtKjøreliste = innsendtKjøreliste)
        kjørelisteJournalpostValidering.validerJournalpost(behandlingId, request.journalpostId)

        return kjørelisteService.lagre(
            innsendtKjøreliste = innsendtKjøreliste,
            fagsakId = behandling.fagsakId,
            journalpostId = request.journalpostId,
            begrunnelse = request.begrunnelse,
            behandlingId = behandlingId,
            manueltRegistrert = true,
        )
    }

    fun slettManuellKjøreliste(
        behandlingId: BehandlingId,
        kjørelisteId: KjørelisteId,
    ) {
        val kjørelisteSomSkalSlettes = kjørelisteService.hentKjøreliste(kjørelisteId)

        brukerfeilHvisIkke(kjørelisteSomSkalSlettes.manueltLagretIBehandling == behandlingId) {
            "Kan ikke slette en kjøreliste som ikke er innsendt manuelt i denne behandlingen"
        }

        avklartKjørelisteService.slettAvklartKjøreliste(kjørelisteSomSkalSlettes.id)
        kjørelisteService.slettKjøreliste(kjørelisteSomSkalSlettes)
    }

    fun oppdaterUke(
        behandlingId: BehandlingId,
        kjørelisteId: KjørelisteId,
        ukeFom: LocalDate,
        dager: List<KjørelisteDag>,
    ): ManueltInnsendtKjørelisteUkeDto {
        val kjøreliste = kjørelisteService.hentKjøreliste(kjørelisteId)

        brukerfeilHvisIkke(kjøreliste.manueltLagretIBehandling == behandlingId) {
            "Kan ikke endre en kjøreliste som ikke er manuelt registrert i denne behandlingen"
        }

        val uke = ukeFom.tilUkeIÅr()
        brukerfeilHvis(dager.any { it.dato.tilUkeIÅr() != uke }) {
            "Alle dager må tilhøre uke ${uke.ukenummer}"
        }

        val behandling = behandlingService.hentBehandling(behandlingId)
        val rammevedtakForReise =
            dagligReisePrivatBilService.hentRammevedtakForReiseIBehandling(behandling.id, kjøreliste.data.reiseId)
        val andreKjørelister = kjørelisteService.hentForFagsakId(behandling.fagsakId).filter { it.id != kjørelisteId }

        validerManuellKjøreliste(
            innsendtKjøreliste = InnsendtKjøreliste(reiseId = kjøreliste.data.reiseId, reisedager = dager),
            rammevedtakForReise = rammevedtakForReise,
            eksisterendeKjørelister = andreKjørelister,
        )

        val oppdaterteReisedager = kjøreliste.data.reisedager.filter { it.dato.tilUkeIÅr() != uke } + dager
        val oppdatertKjøreliste =
            kjørelisteService.oppdater(
                kjøreliste.copy(data = kjøreliste.data.copy(reisedager = oppdaterteReisedager)),
            )

        avklartKjørelisteService.slettAvklartKjøreliste(kjørelisteId)
        avklartKjørelisteService.avklarUkerFraKjøreliste(behandlingId, oppdatertKjøreliste)

        return dager.sortedBy { it.dato }.let { sortert ->
            ManueltInnsendtKjørelisteUkeDto(
                ukenummer = uke.ukenummer,
                fom = sortert.minOf { it.dato },
                tom = sortert.maxOf { it.dato },
                dager = sortert,
            )
        }
    }

    fun oppdaterBegrunnelse(
        behandlingId: BehandlingId,
        kjørelisteId: KjørelisteId,
        begrunnelse: String?,
    ) {
        val kjøreliste = kjørelisteService.hentKjøreliste(kjørelisteId)

        brukerfeilHvisIkke(kjøreliste.manueltLagretIBehandling == behandlingId) {
            "Kan ikke endre en kjøreliste som ikke er manuelt registrert i denne behandlingen"
        }

        kjørelisteService.oppdater(kjøreliste.copy(begrunnelse = begrunnelse))
    }

    /**
     * Avklarer uker fra kjørelister dersom
     * 1. kjørelisten er manuelt registrert i denne behandlingen
     * 2. kjørelisten ikke tidligere har blitt avklart
     *
     * Tar ikke hensyn til endringer på kjøreliste (foreløpig ikke mulig)
     */
    fun avklarNyeKjørelisterManueltRegistrertIBehandling(saksbehandling: Saksbehandling) {
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
        val kjørelisterRegistrertIDenneBehandlingen = kjørelister.filter { it.manueltLagretIBehandling == behandlingId }

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

    private fun validerManuellKjøreliste(
        behandling: Behandling,
        innsendtKjøreliste: InnsendtKjøreliste,
    ) {
        val rammevedtakForReise =
            dagligReisePrivatBilService.hentRammevedtakForReiseIBehandling(behandling.id, innsendtKjøreliste.reiseId)
        val eksisterendeKjørelister = kjørelisteService.hentForFagsakId(behandling.fagsakId)

        validerManuellKjøreliste(
            innsendtKjøreliste = innsendtKjøreliste,
            rammevedtakForReise = rammevedtakForReise,
            eksisterendeKjørelister = eksisterendeKjørelister,
        )
    }
}
