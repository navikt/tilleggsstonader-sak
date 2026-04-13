package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@Service
class AvklartKjørelisteService(
    private val vedtakService: VedtakService,
    private val avklartKjørtUkeRepository: AvklartKjørtUkeRepository,
    private val kjørelisteService: KjørelisteService,
) {
    fun hentAvklarteUkerForBehandling(behandlingId: BehandlingId): List<AvklartKjørtUke> =
        avklartKjørtUkeRepository.findByBehandlingId(behandlingId)

    fun hentAvklartUke(ukeId: UUID): AvklartKjørtUke = avklartKjørtUkeRepository.findByIdOrThrow(ukeId)

    fun avklarUkerFraKjøreliste(
        behandling: Behandling,
        kjøreliste: Kjøreliste,
    ) {
        val rammeForReise = henteReiseFraVedtak(behandling.id, kjøreliste.data.reiseId)

        validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(rammeForReise, kjøreliste)

        val kjørelisteGruppertPåUker = kjøreliste.data.reisedager.groupBy { it.dato.tilUkeIÅr() }

        val avklarteUker =
            kjørelisteGruppertPåUker.map { (ukeIÅr, reisedager) ->
                utledAvklartUke(
                    behandlingId = behandling.id,
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
            ukeSomSkalOppdateres = eksisterendeUke.fom.tilUkeIÅr(),
            rammevedtak = rammevedtak,
            innsendteKjørelisteDager = innsendteKjørelisteDager,
        )

        return avklartKjørtUkeRepository.update(
            eksisterendeUke.copy(
                status = UkeStatus.OK_MANUELT,
                behandletDato = LocalDate.now(),
                dager = oppdaterteDager.toSet(),
            ),
        )
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
        kjørelisteId: UUID,
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
            ukenummer = ukeIÅr.ukenummer,
            // Trengs denne? Kan lages i visningslogikk
            // Rart at den er avhengig av både ukeavvik og dagavvik
            status = utledAutomatiskStatusForUke(avklarteDager, avvikUke),
            typeAvvik = avvikUke,
            behandletDato = null,
            dager = avklarteDager.toSet(),
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
        )
    }

    private fun utledAvvik(kjørelisteDag: KjørelisteDag): List<TypeAvvikDag> =
        listOfNotNull(
            TypeAvvikDag.FOR_HØY_PARKERINGSUTGIFT.takeIf { kjørelisteDag.parkeringsutgift != null && kjørelisteDag.parkeringsutgift > 100 },
            TypeAvvikDag.HELLIDAG_ELLER_HELG.takeIf { kjørelisteDag.harKjørt && kjørelisteDag.dato.erHelg() },
        )

    // TODO: Bruk fra kontrakter
    private fun LocalDate.erHelg() = this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

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
        behandling: Behandling,
        behandlingIdForGjenbruk: BehandlingId,
    ) {
        val avklarteUkerForrigeBehandling = hentAvklarteUkerForBehandling(behandlingIdForGjenbruk)
        val avklarteUkerNyBehandling = hentAvklarteUkerForBehandling(behandling.id)

        val kjørelisterSomFinneIForrigeBehandling = avklarteUkerForrigeBehandling.map { it.kjørelisteId }.toSet()
        val kjørelisterSomFinneINyMenIkkeGammelBehandling =
            avklarteUkerNyBehandling
                .map { it.kjørelisteId }
                .filterNot { kjørelisterSomFinneIForrigeBehandling.contains(it) }
                .map { kjørelisteService.hentKjøreliste(it) }

        // Sletter evt eksisterende avklarte uker på ny behandling
        avklartKjørtUkeRepository.deleteAll(avklarteUkerNyBehandling)

        // Kopier over avklarte uker fra forrige behandling
        val nyeAvklarteUker = avklarteUkerForrigeBehandling.map { it.kopierTilNyBehandling(behandling.id) }
        avklartKjørtUkeRepository.insertAll(nyeAvklarteUker)

        // Avklar nye kjørelister på nytt
        kjørelisterSomFinneINyMenIkkeGammelBehandling.forEach {
            avklarUkerFraKjøreliste(behandling, it)
        }
    }
}
