package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import io.github.mikaojk.holiday.getNorwegianHolidays
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.springframework.stereotype.Service
import java.time.DayOfWeek
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
            oppdaterteDager = oppdaterteDager,
            ukeSomSkalOppdateres = eksisterendeUke.uke,
            rammevedtak = rammevedtak,
            innsendteKjørelisteDager = innsendteKjørelisteDager,
            tillatOverskridelseRammevedtak = unleashService.isEnabled(Toggle.KAN_OVERSKRIDE_ANTALL_DAGER_I_RAMMEVEDTAK),
        )

        val nyAvklartKjørtUkeStatus = beregnNyStatus(behandlingId, eksisterendeUke, oppdaterteDager)

        return avklartKjørtUkeRepository.update(
            eksisterendeUke.copy(
                status = UkeStatus.OK_MANUELT,
                behandletDato = LocalDate.now(),
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
            behandlingService.hentBehandling(behandlingId).forrigeIverksatteBehandlingId
                ?: return null
        return hentAvklarteUkerForBehandling(forrigeBehandlingId)
            .find { it.uke == uke.uke && it.reiseId == uke.reiseId }
    }

    private fun oppdaterAvklarteDager(
        eksisterendeDager: Collection<AvklartKjørtDag>,
        oppdaterteDager: Collection<EndreAvklartDagRequest>,
    ): List<AvklartKjørtDag> =
        eksisterendeDager
            .associateWith { eksisterendeDag -> oppdaterteDager.find { it.dato == eksisterendeDag.dato } }
            .map { (eksisterendeDag, oppdatertDag) ->
                feilHvis(oppdatertDag == null) { "Alle dager i uke må sendes inn" }

                eksisterendeDag.copy(
                    godkjentGjennomførtKjøring = oppdatertDag.godkjentGjennomførtKjøring,
                    parkeringsutgift = oppdatertDag.parkeringsutgift,
                    begrunnelse = oppdatertDag.begrunnelse,
                )
            }

    private fun utledGodkjentGjennomførtKjøringAutomatisk(
        harKjørt: Boolean,
        ukeEllerDagHarAvvik: Boolean,
    ): GodkjentGjennomførtKjøring =
        if (!harKjørt) {
            GodkjentGjennomførtKjøring.NEI
        } else if (!ukeEllerDagHarAvvik) {
            GodkjentGjennomførtKjøring.JA
        } else {
            GodkjentGjennomførtKjøring.IKKE_VURDERT
        }

    private fun utledAvklartUke(
        behandlingId: BehandlingId,
        kjørelisteId: KjørelisteId,
        ukeIÅr: UkeIÅr,
        reisedager: List<KjørelisteDag>,
        rammevedtak: RammeForReiseMedPrivatBil,
    ): AvklartKjørtUke {
        val delperiodeForUke =
            rammevedtak.finnDelperiodeForPeriode(
                Datoperiode(reisedager.minOf { it.dato }, reisedager.maxOf { it.dato }),
            )
        val avvikUke =
            if (!vurderAntallDagerInnenforRamme(reisedager, delperiodeForUke)) {
                TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK
            } else {
                null
            }

        val avklarteDager = reisedager.map { utledAvklartDag(it, avvikUke) }

        return AvklartKjørtUke(
            behandlingId = behandlingId,
            kjørelisteId = kjørelisteId,
            reiseId = rammevedtak.reiseId,
            fom = reisedager.minOf { it.dato },
            tom = reisedager.maxOf { it.dato },
            uke = ukeIÅr,
            // Trengs denne? Kan lages i visningslogikk
            // Rart at den er avhengig av både ukeavvik og dagavvik
            status = utledAutomatiskStatusForUke(avklarteDager, avvikUke),
            typeAvvik = avvikUke,
            behandletDato = null,
            dager = avklarteDager.toSet(),
            avklartKjørtUkeStatus = AvklartKjørtUkeStatus.NY,
        )
    }

    private fun vurderAntallDagerInnenforRamme(
        dager: List<KjørelisteDag>,
        delperiodeForUke: RammeForReiseMedPrivatBilDelperiode,
    ): Boolean {
        val antallDagerMedUtbetaling = dager.filter { it.harKjørt }.size

        return antallDagerMedUtbetaling <= delperiodeForUke.reisedagerPerUke
    }

    private fun utledAutomatiskStatusForUke(
        avklarteDager: List<AvklartKjørtDag>,
        avvikUke: TypeAvvikUke?,
    ): UkeStatus {
        if (avvikUke != null) return UkeStatus.AVVIK

        val automatiskeVurderingForDager = avklarteDager.map { it.automatiskVurdering }.toSet()

        // Antar at man må sende inn en hel kjøreliste
        if (automatiskeVurderingForDager.size == 1 && automatiskeVurderingForDager.single() == UtfyltDagAutomatiskVurdering.OK) {
            return UkeStatus.OK_AUTOMATISK
        }

        return UkeStatus.AVVIK
    }

    private fun utledAvklartDag(
        kjørelisteDag: KjørelisteDag,
        avvikUke: TypeAvvikUke?,
    ): AvklartKjørtDag {
        val avvik = utledAvvik(kjørelisteDag)

        val godkjentGjennomførtKjøring =
            utledGodkjentGjennomførtKjøringAutomatisk(
                harKjørt = kjørelisteDag.harKjørt,
                ukeEllerDagHarAvvik = (avvik.isNotEmpty() || avvikUke != null),
            )

        return AvklartKjørtDag(
            dato = kjørelisteDag.dato,
            godkjentGjennomførtKjøring = godkjentGjennomførtKjøring,
            avvik = avvik,
            automatiskVurdering = if (avvik.isEmpty()) UtfyltDagAutomatiskVurdering.OK else UtfyltDagAutomatiskVurdering.AVVIK,
            begrunnelse = null,
            parkeringsutgift = if (godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.JA) kjørelisteDag.parkeringsutgift else null,
            avklartKjørtDagStatus = AvklartKjørtDagStatus.NY,
        )
    }

    private fun utledAvvik(kjørelisteDag: KjørelisteDag): List<TypeAvvikDag> =
        listOfNotNull(
            TypeAvvikDag.FOR_HØY_PARKERINGSUTGIFT.takeIf { kjørelisteDag.parkeringsutgift != null && kjørelisteDag.parkeringsutgift > 100 },
            TypeAvvikDag.HELLIDAG_ELLER_HELG.takeIf { kjørelisteDag.harKjørt && kjørelisteDag.dato.erHelgEllerHelligdag() },
        )

    private fun LocalDate.erHelgEllerHelligdag() =
        this.dayOfWeek == DayOfWeek.SATURDAY ||
            this.dayOfWeek == DayOfWeek.SUNDAY ||
            getNorwegianHolidays(year).map { it.date }.contains(this)

    private fun henteReiseFraVedtak(
        behandlingId: BehandlingId,
        reiseId: ReiseId,
    ): RammeForReiseMedPrivatBil {
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
                    val sisteDagIRammevedtak = rammevedtakForReise.grunnlag.tom
                    when {
                        uke.fom > sisteDagIRammevedtak -> uke.markerHeleUkaSomSlettet()
                        uke.inneholder(sisteDagIRammevedtak) -> uke.markerDelerAvUkaSomSlettet(fraDato = sisteDagIRammevedtak)
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

    private fun AvklartKjørtUke.markerDelerAvUkaSomSlettet(fraDato: LocalDate): AvklartKjørtUke? {
        val oppdaterteDager = dager.markerSomSlettet(fraDato)
        return if (oppdaterteDager != dager) {
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
    }
}
