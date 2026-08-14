package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class AvklartKjørelisteService(
    private val vedtakService: VedtakService,
    private val avklartKjørtUkeRepository: AvklartKjørtUkeRepository,
    private val kjørelisteService: KjørelisteService,
    private val behandlingService: BehandlingService,
    private val unleashService: UnleashService,
) {
    fun hentAvklarteUkerForBehandling(behandlingId: BehandlingId): List<AvklartKjørtUke> =
        avklartKjørtUkeRepository.findByBehandlingId(behandlingId)

    fun hentAvklartUke(ukeId: UUID): AvklartKjørtUke = avklartKjørtUkeRepository.findByIdOrThrow(ukeId)

    fun avklarUkerFraKjøreliste(
        behandlingId: BehandlingId,
        kjøreliste: Kjøreliste,
    ) {
        val rammeForReise = henteReiseFraVedtak(behandlingId, kjøreliste.data.reiseId)

        validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(rammeForReise, kjøreliste)

        val kjørelisteGruppertPåUker = kjøreliste.data.reisedager.groupBy { it.dato.tilUkeIÅr() }

        val avklarteUker =
            kjørelisteGruppertPåUker.map { (ukeIÅr, reisedager) ->
                utledAvklartUke(
                    behandlingId = behandlingId,
                    ukeIÅr = ukeIÅr,
                    reisedager = reisedager,
                    kjørelisteId = kjøreliste.id,
                    rammevedtak = rammeForReise,
                )
            }

        avklartKjørtUkeRepository.insertAll(avklarteUker)
    }

    fun oppdaterAvklartUke(
        behandlingId: BehandlingId,
        ukeId: UUID,
        request: List<EndreAvklartDagRequest>,
    ): AvklartKjørtUke {
        val eksisterendeUke = hentAvklartUke(ukeId)
        val oppdaterteDager = oppdaterAvklarteDager(eksisterendeUke.dager, request)

        val rammevedtak = henteReiseFraVedtak(behandlingId, eksisterendeUke.reiseId)
        val innsendteKjørelisteDager = kjørelisteService.hentKjøreliste(eksisterendeUke.kjørelisteId).data.reisedager

        validerOppdatertAvklartKjørtUke(
            oppdaterteDager = oppdaterteDager.filter { it.avklartKjørtDagStatus != AvklartKjørtDagStatus.SLETTET },
            ukeSomSkalOppdateres = eksisterendeUke.uke,
            rammevedtak = rammevedtak,
            innsendteKjørelisteDager = innsendteKjørelisteDager,
            tillatOverskridelseRammevedtak = unleashService.isEnabled(Toggle.KAN_OVERSKRIDE_ANTALL_DAGER_I_RAMMEVEDTAK),
        )

        val nyAvklartKjørtUkeStatus = beregnNyStatus(behandlingId, eksisterendeUke, oppdaterteDager)

        return avklartKjørtUkeRepository.update(
            eksisterendeUke.copy(
                status = UkeStatus.OK_MANUELT,
                dager = oppdaterteDager.toSet(),
                avklartKjørtUkeStatus = nyAvklartKjørtUkeStatus,
            ),
        )
    }

    private fun beregnNyStatus(
        behandlingId: BehandlingId,
        eksisterendeUke: AvklartKjørtUke,
        oppdaterteDager: Collection<AvklartKjørtDag>,
    ): AvklartKjørtUkeStatus =
        when (eksisterendeUke.avklartKjørtUkeStatus) {
            AvklartKjørtUkeStatus.UENDRET ->
                if (erDagerEndret(eksisterendeUke.dager, oppdaterteDager)) {
                    AvklartKjørtUkeStatus.ENDRET
                } else {
                    AvklartKjørtUkeStatus.UENDRET
                }

            AvklartKjørtUkeStatus.ENDRET -> {
                val forrigeUke = hentForrigeBehandlingsUke(behandlingId, eksisterendeUke)
                if (forrigeUke != null && !erDagerEndret(forrigeUke.dager, oppdaterteDager)) {
                    AvklartKjørtUkeStatus.UENDRET
                } else {
                    AvklartKjørtUkeStatus.ENDRET
                }
            }

            AvklartKjørtUkeStatus.NY -> AvklartKjørtUkeStatus.NY
            AvklartKjørtUkeStatus.SLETTET -> AvklartKjørtUkeStatus.SLETTET
        }

    private fun erDagerEndret(
        eksisterendeDager: Collection<AvklartKjørtDag>,
        oppdaterteDager: Collection<AvklartKjørtDag>,
    ): Boolean {
        val eksisterendePerDato = eksisterendeDager.associateBy { it.dato }
        return oppdaterteDager.any { oppdatert ->
            val eksisterende = eksisterendePerDato[oppdatert.dato]
            eksisterende == null ||
                eksisterende.godkjentGjennomførtKjøring != oppdatert.godkjentGjennomførtKjøring ||
                eksisterende.parkeringsutgift != oppdatert.parkeringsutgift ||
                eksisterende.begrunnelse != oppdatert.begrunnelse
        }
    }

    private fun hentForrigeBehandlingsUke(
        behandlingId: BehandlingId,
        uke: AvklartKjørtUke,
    ): AvklartKjørtUke? {
        val forrigeBehandlingId =
            behandlingService.hentBehandling(behandlingId).forrigeIverksatteBehandlingId ?: return null

        return hentAvklarteUkerForBehandling(forrigeBehandlingId)
            .find { it.uke == uke.uke && it.reiseId == uke.reiseId }
    }

    private fun oppdaterAvklarteDager(
        eksisterendeDager: Collection<AvklartKjørtDag>,
        oppdaterteDager: Collection<EndreAvklartDagRequest>,
    ): List<AvklartKjørtDag> {
        val ikkeSlettedeDatoer = eksisterendeDager.filterNot { it.erSlettet() }.map { it.dato }.toSet()
        val oppdaterteDagerPerDato = oppdaterteDager.groupBy { it.dato }

        brukerfeilHvis(oppdaterteDagerPerDato.any { (_, oppdaterteDager) -> oppdaterteDager.size > 1 }) {
            "Kan ikke sende inn duplikate dager"
        }

        val oppdaterteDagerDatoer = oppdaterteDagerPerDato.keys

        brukerfeilHvis(oppdaterteDagerDatoer != ikkeSlettedeDatoer) {
            "Alle dager i uken må sendes inn"
        }

        return eksisterendeDager.map { eksisterendeDag ->
            if (eksisterendeDag.erSlettet()) {
                eksisterendeDag
            } else {
                val oppdatertDag = oppdaterteDagerPerDato.getValue(eksisterendeDag.dato).single()
                eksisterendeDag.copy(
                    godkjentGjennomførtKjøring = oppdatertDag.godkjentGjennomførtKjøring,
                    parkeringsutgift = oppdatertDag.parkeringsutgift,
                    begrunnelse = oppdatertDag.begrunnelse,
                )
            }
        }
    }

    private fun henteReiseFraVedtak(
        behandlingId: BehandlingId,
        reiseId: ReiseId,
    ): RammevedtakForReiseMedPrivatBil {
        val rammevedtak =
            vedtakService
                .hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandlingId)
                .data
                .rammevedtakPrivatBil
                ?: error("Fant ikke rammevedtak for behandling med id $behandlingId")

        return rammevedtak.reiser.singleOrNull { it.reiseId == reiseId }
            ?: error("Forventet å finne ramme for reise med id $reiseId")
    }

    fun nullstillOgGjenbrukAvklarteUker(
        behandlingId: BehandlingId,
        behandlingIdForGjenbruk: BehandlingId,
    ) {
        val avklarteUkerForrigeBehandling = hentAvklarteUkerForBehandling(behandlingIdForGjenbruk)
        val avklarteUkerNyBehandling = hentAvklarteUkerForBehandling(behandlingId)

        val kjørelisterSomFinneIForrigeBehandling = avklarteUkerForrigeBehandling.map { it.kjørelisteId }.toSet()
        val kjørelisterSomFinneINyMenIkkeGammelBehandling =
            avklarteUkerNyBehandling
                .map { it.kjørelisteId }
                .filterNot { kjørelisterSomFinneIForrigeBehandling.contains(it) }
                .distinct() // En kjøreliste kan dekke flere uker
                .map { kjørelisteService.hentKjøreliste(it) }

        // Sletter evt eksisterende avklarte uker på ny behandling
        avklartKjørtUkeRepository.deleteAll(avklarteUkerNyBehandling)

        // Kopier over avklarte uker fra forrige behandling
        val avklarteUkerFraForrigeBehandling =
            avklarteUkerForrigeBehandling
                .filter { it.avklartKjørtUkeStatus != AvklartKjørtUkeStatus.SLETTET }
                .map { it.kopierTilNyBehandling(behandlingId) }
        avklartKjørtUkeRepository.insertAll(avklarteUkerFraForrigeBehandling)

        // Avklar nye kjørelister på nytt
        kjørelisterSomFinneINyMenIkkeGammelBehandling.forEach {
            avklarUkerFraKjøreliste(behandlingId, it)
        }
    }

    fun sletteMarkerUkerOgDagerUtenforAvkortetRammevedtak(
        behandlingId: BehandlingId,
        rammevedtak: RammevedtakPrivatBil?,
    ) {
        val oppdaterteUker =
            hentAvklarteUkerForBehandling(behandlingId).mapNotNull { uke ->
                val rammevedtakForReise = rammevedtak?.reiser?.find { it.reiseId == uke.reiseId }
                if (rammevedtakForReise == null) {
                    uke.markerHeleUkaSomSlettet()
                } else {
                    val grunnlag = rammevedtakForReise.grunnlag
                    when {
                        !uke.overlapper(grunnlag) -> uke.markerHeleUkaSomSlettet()
                        !grunnlag.inneholder(uke) -> uke.markerDelerAvUkaSomSlettet(gyldigPeriode = grunnlag)
                        else -> null
                    }
                }
            }

        if (oppdaterteUker.isNotEmpty()) {
            avklartKjørtUkeRepository.updateAll(oppdaterteUker)
        }
    }

    private fun AvklartKjørtUke.markerHeleUkaSomSlettet(): AvklartKjørtUke =
        copy(
            avklartKjørtUkeStatus = AvklartKjørtUkeStatus.SLETTET,
            dager = dager.markerSomSlettet(),
        )

    private fun AvklartKjørtUke.markerDelerAvUkaSomSlettet(gyldigPeriode: Periode<LocalDate>): AvklartKjørtUke? {
        val oppdaterteDager = dager.markerSomSlettetUtenforPeriode(gyldigPeriode = gyldigPeriode)
        return oppdaterUkeHvisDagerErEndret(oppdaterteDager)
    }

    private fun AvklartKjørtUke.oppdaterUkeHvisDagerErEndret(oppdaterteDager: Set<AvklartKjørtDag>): AvklartKjørtUke? =
        if (oppdaterteDager != dager) {
            val nyUkeStatus =
                if (avklartKjørtUkeStatus == AvklartKjørtUkeStatus.UENDRET) {
                    AvklartKjørtUkeStatus.ENDRET
                } else {
                    avklartKjørtUkeStatus
                }
            copy(dager = oppdaterteDager, avklartKjørtUkeStatus = nyUkeStatus)
        } else {
            null
        }

    fun slettAvklarteUkerOgKjørelisterLagtTilManueltIBehandling(behandlingId: BehandlingId) {
        val kjørelisterLagretIBehandling = kjørelisteService.hentManueltLagredeIBehandling(behandlingId)

        kjørelisterLagretIBehandling.forEach {
            avklartKjørtUkeRepository.deleteAvklartKjørtUkesByKjørelisteId(it.id)
        }

        kjørelisteService.slettKjørelister(kjørelisterLagretIBehandling)
    }

    fun slettAvklartKjøreliste(kjørelisteId: KjørelisteId) {
        avklartKjørtUkeRepository.deleteAvklartKjørtUkesByKjørelisteId(kjørelisteId)
    }
}
