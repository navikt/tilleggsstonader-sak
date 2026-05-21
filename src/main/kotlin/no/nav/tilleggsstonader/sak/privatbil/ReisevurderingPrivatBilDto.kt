package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UtfyltDagAutomatiskVurdering
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.RammeForReiseMedPrivatBilDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate
import java.util.UUID

data class ReisevurderingPrivatBilDto(
    val reiseId: ReiseId,
    val rammevedtak: RammeForReiseMedPrivatBilDto,
    val uker: List<UkeVurderingDto>,
) {
    constructor(
        reise: RammeForReiseMedPrivatBil,
        avklarteUker: List<AvklartKjørtUke>,
        kjørelister: List<Kjøreliste>,
    ) : this(
        reiseId = reise.reiseId,
        rammevedtak = reise.tilDto(),
        uker =
            reise.grunnlag
                .alleDatoerGruppertPåUke()
                .map { (uke, datoer) ->
                    val avklartUke = avklarteUker.singleOrNull { it.reiseId == reise.reiseId && it.uke == uke }
                    val kjørelisteForUke = avklartUke?.let { kjørelister.firstOrNull { it.id == avklartUke.kjørelisteId } }
                    UkeVurderingDto(uke = uke, datoer = datoer, kjørelisteForUke = kjørelisteForUke, avklartUke = avklartUke)
                },
    )
}

data class UkeVurderingDto(
    val ukenummer: Int,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val status: UkeStatus,
    val avvik: AvvikUke?,
    val behandletDato: LocalDate?,
    val kjørelisteInnsendtDato: LocalDate?, // null hvis kjøreliste ikke er mottatt
    val kjørelisteId: KjørelisteId?, // null hvis kjøreliste ikke er mottatt
    val avklartUkeId: UUID?,
    val avklartKjørtUkeStatus: AvklartKjørtUkeStatus?, // null hvis avklartKjørtUke ikke finnes
    val dager: List<DagDto>,
) {
    constructor(
        uke: UkeIÅr,
        datoer: List<LocalDate>,
        kjørelisteForUke: Kjøreliste?,
        avklartUke: AvklartKjørtUke?,
    ) : this(
        ukenummer = uke.ukenummer,
        fraDato = datoer.min(),
        tilDato = datoer.max(),
        status = avklartUke?.status ?: UkeStatus.IKKE_MOTTATT_KJØRELISTE,
        avvik =
            avklartUke?.typeAvvik?.let {
                AvvikUke(
                    typeAvvik = it,
                    avviksMelding = "Dette er egentlig ikke et avvik, bare en test",
                )
            },
        behandletDato = avklartUke?.behandletDato,
        kjørelisteInnsendtDato = kjørelisteForUke?.datoMottatt?.toLocalDate(),
        kjørelisteId = kjørelisteForUke?.id,
        avklartUkeId = avklartUke?.id,
        avklartKjørtUkeStatus = avklartUke?.avklartKjørtUkeStatus,
        dager = datoer.map { dato -> DagDto(dato = dato, kjørelisteForUke = kjørelisteForUke, avklartUke = avklartUke) },
    )

    companion object {
        fun fraAvklartUke(
            avklartUke: AvklartKjørtUke,
            kjøreliste: Kjøreliste?,
        ): UkeVurderingDto =
            UkeVurderingDto(
                uke = avklartUke.uke,
                datoer = avklartUke.alleDatoer(),
                kjørelisteForUke = kjøreliste,
                avklartUke = avklartUke,
            )
    }
}

data class AvvikUke(
    val typeAvvik: TypeAvvikUke,
    val avviksMelding: String,
)

data class DagDto(
    val dato: LocalDate,
    val ukedag: String, // avklar om faktisk trenger, eller om frontend skal mappe ut fra dag
    val kjørelisteDag: KjørelisteDagDto?,
    val avklartDag: AvklartDagDto?,
) {
    constructor(
        dato: LocalDate,
        kjørelisteForUke: Kjøreliste?,
        avklartUke: AvklartKjørtUke?,
    ) : this(
        dato = dato,
        ukedag = dato.dayOfWeek.name,
        kjørelisteDag =
            kjørelisteForUke
                ?.data
                ?.reisedager
                ?.firstOrNull { it.dato == dato }
                ?.let { reisedag ->
                    KjørelisteDagDto(
                        harKjørt = reisedag.harKjørt,
                        parkeringsutgift = reisedag.parkeringsutgift,
                    )
                },
        // TODO: Vurder å kaste feil dersom denne er null
        // Hvis den eksisterer for en uke så burde alle dager eksistere?
        avklartDag = avklartUke?.dager?.singleOrNull { it.dato == dato }?.tilDto(),
    )
}

data class KjørelisteDagDto(
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)

data class AvklartDagDto(
    val godkjentGjennomførtKjøring: GodkjentGjennomførtKjøring,
    val automatiskVurdering: UtfyltDagAutomatiskVurdering,
    val avvik: List<TypeAvvikDag>,
    val begrunnelse: String?, // må fylles ut om avvik?
    val parkeringsutgift: Int?,
)

fun AvklartKjørtDag.tilDto() =
    AvklartDagDto(
        godkjentGjennomførtKjøring = this.godkjentGjennomførtKjøring,
        automatiskVurdering = this.automatiskVurdering,
        avvik = this.avvik,
        begrunnelse = this.begrunnelse,
        parkeringsutgift = this.parkeringsutgift,
    )
