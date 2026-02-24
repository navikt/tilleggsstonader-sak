package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForUke
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
) {
    fun hentAvklarteUkerForBehandling(behandlingId: BehandlingId): List<AvklartKjørtUke> =
        avklartKjørtUkeRepository.findByBehandlingId(behandlingId)

    fun avklarUkerFraKjøreliste(
        behandling: Saksbehandling,
        kjøreliste: Kjøreliste,
    ) {
        val rammeForReise = hentReiseFraForrigeVedtak(behandling, kjøreliste.data.reiseId)

        validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(rammeForReise, kjøreliste)

        val kjørelisteGruppertPåUker = kjøreliste.data.reisedager.groupBy { it.dato.ukenummer() }

        val avklarteUker =
            kjørelisteGruppertPåUker.map { (ukenummer, reisedager) ->
                utledAvklartUke(
                    behandlingId = behandling.id,
                    ukenummer = ukenummer,
                    reisedager = reisedager,
                    kjørelisteId = kjøreliste.id,
                    rammevedtakForUke = finnRammevedtakForUke(rammeForReise, ukenummer),
                )
            }

        avklartKjørtUkeRepository.insertAll(avklarteUker)
    }

    private fun utledAvklartUke(
        behandlingId: BehandlingId,
        kjørelisteId: UUID,
        ukenummer: Int,
        reisedager: List<KjørelisteDag>,
        rammevedtakForUke: RammeForUke,
    ): AvklartKjørtUke {
        val avklarteDager = reisedager.map { utledAvklartDag(it) }

        val avvik =
            if (!vurderAntallDagerInnenforRamme(reisedager, rammevedtakForUke)) {
                TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK
            } else {
                null
            }

        return AvklartKjørtUke(
            behandlingId = behandlingId,
            kjørelisteId = kjørelisteId,
            fom = reisedager.minOf { it.dato },
            tom = reisedager.maxOf { it.dato },
            ukenummer = ukenummer,
            // Trengs denne? Kan lages i visningslogikk
            // Rart at den er avhengig av både ukeavvik og dagavvik
            status = utledStatusForUke(avklarteDager, avvik),
            typeAvvik = avvik,
            behandletDato = null,
            dager = avklarteDager.toSet(),
        )
    }

    private fun vurderAntallDagerInnenforRamme(
        dager: List<KjørelisteDag>,
        rammevedtakForUke: RammeForUke,
    ): Boolean {
        val antallDagerMedUtbetaling = dager.filter { it.harKjørt }.size

        return antallDagerMedUtbetaling <= rammevedtakForUke.grunnlag.maksAntallDagerSomKanDekkes
    }

    private fun utledStatusForUke(
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

    private fun utledAvklartDag(kjørelisteDag: KjørelisteDag): AvklartKjørtDag {
        val avvik = utledAvvik(kjørelisteDag)

        return AvklartKjørtDag(
            dato = kjørelisteDag.dato,
            godkjentGjennomførtKjøring = kjørelisteDag.harKjørt,
            avvik = avvik,
            automatiskVurdering = if (avvik.isEmpty()) UtfyltDagAutomatiskVurdering.OK else UtfyltDagAutomatiskVurdering.AVVIK,
            begrunnelse = null,
            parkeringsutgift = kjørelisteDag.parkeringsutgift,
        )
    }

    private fun utledAvvik(kjørelisteDag: KjørelisteDag): List<TypeAvvikDag> =
        listOfNotNull(
            TypeAvvikDag.FOR_HØY_PARKERINGSUTGIFT.takeIf { kjørelisteDag.parkeringsutgift != null && kjørelisteDag.parkeringsutgift > 100 },
            TypeAvvikDag.HELLIDAG_ELLER_HELG.takeIf { kjørelisteDag.harKjørt && kjørelisteDag.dato.erHelg() },
        )

    // TODO: Bruk fra kontrakter
    private fun LocalDate.erHelg() = this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

    private fun validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(
        rammeForReise: RammeForReiseMedPrivatBil,
        kjøreliste: Kjøreliste,
    ) {
        feilHvisIkke(rammeForReise.grunnlag.inneholder(kjøreliste.data)) {
            "Kjøreliste er ikke innenfor rammevedtaket"
        }
    }

    private fun hentReiseFraForrigeVedtak(
        behandling: Saksbehandling,
        reiseId: ReiseId,
    ): RammeForReiseMedPrivatBil {
        val rammeFraForrigeBehandling =
            behandling.forrigeIverksatteBehandlingId
                ?.let {
                    vedtakService
                        .hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandling.forrigeIverksatteBehandlingId)
                }?.data
                ?.rammevedtakPrivatBil
                ?: error("Fant ikke rammevedtak for forrige behandling med id ${behandling.forrigeIverksatteBehandlingId}")

        return rammeFraForrigeBehandling.reiser.singleOrNull { it.reiseId == reiseId }
            ?: error("Forventet å finne ramme for reise med id $reiseId")
    }

    private fun finnRammevedtakForUke(
        rammeForReise: RammeForReiseMedPrivatBil,
        ukenummer: Int,
    ) = rammeForReise.uker.singleOrNull { it.grunnlag.fom.ukenummer() == ukenummer }
        ?: error("Forventet å finne rammevedtak for uke")
}
