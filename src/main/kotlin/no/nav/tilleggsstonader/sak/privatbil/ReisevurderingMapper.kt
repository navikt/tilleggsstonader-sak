package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import java.time.LocalDate

object ReisevurderingMapper {
    fun RammeForReiseMedPrivatBil.tilReisevurderingDto(
        avklarteUker: List<AvklartKjørtUke>,
        kjørelister: List<Kjøreliste>,
    ): ReisevurderingPrivatBilDto =
        ReisevurderingPrivatBilDto(
            reiseId = reiseId,
            rammevedtak = tilDto(),
            uker =
                grunnlag
                    .alleDatoerGruppertPåUke()
                    .map { (uke, datoer) ->
                        val avklartUke = avklarteUker.singleOrNull { it.reiseId == reiseId && it.uke == uke }
                        val kjørelisteForUke = avklartUke?.let { kjørelister.firstOrNull { it.id == avklartUke.kjørelisteId } }
                        lagUkeVurderingDto(uke = uke, datoer = datoer, avklartUke = avklartUke, kjøreliste = kjørelisteForUke)
                    },
        )

    fun AvklartKjørtUke.tilUkeVurderingDto(kjøreliste: Kjøreliste?): UkeVurderingDto =
        lagUkeVurderingDto(uke = uke, datoer = alleDatoer(), avklartUke = this, kjøreliste = kjøreliste)

    private fun lagUkeVurderingDto(
        uke: UkeIÅr,
        datoer: List<LocalDate>,
        avklartUke: AvklartKjørtUke?,
        kjøreliste: Kjøreliste?,
    ): UkeVurderingDto =
        UkeVurderingDto(
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
            kjørelisteInnsendtDato = kjøreliste?.datoMottatt?.toLocalDate(),
            kjørelisteId = kjøreliste?.id,
            avklartUkeId = avklartUke?.id,
            avklartKjørtUkeStatus = avklartUke?.avklartKjørtUkeStatus,
            dager = datoer.map { dato -> lagDagDto(dato = dato, kjørelisteForUke = kjøreliste, avklartUke = avklartUke) },
        )

    private fun lagDagDto(
        dato: LocalDate,
        kjørelisteForUke: Kjøreliste?,
        avklartUke: AvklartKjørtUke?,
    ): DagDto =
        DagDto(
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
            avklartDag = avklartUke?.dager?.singleOrNull { it.dato == dato }?.tilAvklartDagDto(),
        )

    private fun AvklartKjørtDag.tilAvklartDagDto(): AvklartDagDto =
        AvklartDagDto(
            godkjentGjennomførtKjøring = godkjentGjennomførtKjøring,
            automatiskVurdering = automatiskVurdering,
            avvik = avvik,
            begrunnelse = begrunnelse,
            parkeringsutgift = parkeringsutgift,
        )
}
