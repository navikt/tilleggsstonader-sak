package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import io.github.mikaojk.holiday.getNorwegianHolidays
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import java.time.DayOfWeek
import java.time.LocalDate

fun utledAvklartDag(
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

fun utledAvklartUke(
    behandlingId: BehandlingId,
    kjørelisteId: KjørelisteId,
    ukeIÅr: UkeIÅr,
    reisedager: List<KjørelisteDag>,
    rammevedtak: RammevedtakForReiseMedPrivatBil,
    avklartKjørtUkeStatus: AvklartKjørtUkeStatus = AvklartKjørtUkeStatus.NY,
): AvklartKjørtUke {
    val avvikUke = utledAvvikForUke(rammevedtak, reisedager)

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
        dager = avklarteDager.toSet(),
        avklartKjørtUkeStatus = avklartKjørtUkeStatus,
    )
}

fun utledAvvikForUke(
    rammevedtak: RammevedtakForReiseMedPrivatBil,
    reisedager: List<KjørelisteDag>,
): TypeAvvikUke? {
    val delperiodeForUke =
        rammevedtak.finnDelperiodeForPeriode(
            Datoperiode(reisedager.minOf { it.dato }, reisedager.maxOf { it.dato }),
        )
    return when {
        !erAntallDagerInnenforRamme(reisedager, delperiodeForUke) -> {
            TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK
        }

        else -> {
            null
        }
    }
}

fun utledAutomatiskStatusForUke(
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

private fun utledAvvik(kjørelisteDag: KjørelisteDag): List<TypeAvvikDag> =
    listOfNotNull(
        TypeAvvikDag.FOR_HØY_PARKERINGSUTGIFT.takeIf { kjørelisteDag.parkeringsutgift != null && kjørelisteDag.parkeringsutgift > 100 },
        TypeAvvikDag.HELLIDAG_ELLER_HELG.takeIf { kjørelisteDag.harKjørt && kjørelisteDag.dato.erHelgEllerHelligdag() },
    )

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

private fun LocalDate.erHelgEllerHelligdag() =
    this.dayOfWeek == DayOfWeek.SATURDAY ||
        this.dayOfWeek == DayOfWeek.SUNDAY ||
        getNorwegianHolidays(year).map { it.date }.contains(this)

private fun erAntallDagerInnenforRamme(
    dager: List<KjørelisteDag>,
    delperiodeForUke: RammeForReiseMedPrivatBilDelperiode,
): Boolean {
    val antallDagerMedUtbetaling = dager.filter { it.harKjørt }.size

    return antallDagerMedUtbetaling <= delperiodeForUke.reisedagerPerUke
}
