package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

object ReisevurderingPrivatBilMapper {
    fun tilReisevurderingDto(
        gjeldendeRammevedtakForReise: RammeForReiseMedPrivatBil?,
        forrigeRammevedtakForReise: RammeForReiseMedPrivatBil?,
        avklarteUker: List<AvklartKjørtUke>,
        kjørelister: List<Kjøreliste>,
    ): ReisevurderingPrivatBilDto {
        val reiseId =
            finnReiseId(
                gjeldendeRammevedtakForReise = gjeldendeRammevedtakForReise,
                forrigeRammevedtakForReise = forrigeRammevedtakForReise,
            )
        val ukeVurderingerDto =
            lagUkeVurderingerDto(
                gjeldendeRammevedtakForReise = gjeldendeRammevedtakForReise,
                forrigeRammevedtakForReise = forrigeRammevedtakForReise,
                avklarteUker = avklarteUker,
                kjørelister = kjørelister,
            )
        return ReisevurderingPrivatBilDto(
            reiseId = reiseId,
            rammevedtak = gjeldendeRammevedtakForReise?.tilDto(),
            forrigeRammevedtak = forrigeRammevedtakForReise?.tilDto(),
            uker = ukeVurderingerDto,
        )
    }

    /**
     * Bruker gammelt rammevedtak for å finne ReiseId hvis hele reisen er slettet
     */
    private fun finnReiseId(
        gjeldendeRammevedtakForReise: RammeForReiseMedPrivatBil?,
        forrigeRammevedtakForReise: RammeForReiseMedPrivatBil?,
    ): ReiseId =
        gjeldendeRammevedtakForReise?.reiseId ?: forrigeRammevedtakForReise?.reiseId
            ?: feil("Kan ikke lage reisevudering. Mangler rammevedtak for reise")

    private fun lagUkeVurderingerDto(
        gjeldendeRammevedtakForReise: RammeForReiseMedPrivatBil?,
        forrigeRammevedtakForReise: RammeForReiseMedPrivatBil?,
        avklarteUker: List<AvklartKjørtUke>,
        kjørelister: List<Kjøreliste>,
    ): List<UkeVurderingDto> {
        val reiseId =
            finnReiseId(
                gjeldendeRammevedtakForReise = gjeldendeRammevedtakForReise,
                forrigeRammevedtakForReise = forrigeRammevedtakForReise,
            )
        val gjeldendeUker = gjeldendeRammevedtakForReise?.grunnlag?.alleDatoerGruppertPåUke().orEmpty()
        val forrigeUker = forrigeRammevedtakForReise?.grunnlag?.alleDatoerGruppertPåUke().orEmpty()
        val sammenslåtteUker = (gjeldendeUker.keys + forrigeUker.keys).distinct().sorted()

        return sammenslåtteUker.map { uke ->
            val datoerForUke = (gjeldendeUker[uke].orEmpty() + forrigeUker[uke].orEmpty()).distinct().sorted()
            val avklartUke = avklarteUker.singleOrNull { it.reiseId == reiseId && it.uke == uke }
            val kjørelisteForUke = avklartUke?.let { kjørelister.firstOrNull { it.id == avklartUke.kjørelisteId } }

            lagUkeVurderingDto(
                uke = uke,
                datoer = datoerForUke,
                avklartUke = avklartUke,
                kjøreliste = kjørelisteForUke,
                erUkeSlettet = erUkeSlettet(uke, gjeldendeUker, forrigeUker),
            )
        }
    }

    fun lagUkeVurderingDto(
        uke: UkeIÅr,
        datoer: List<LocalDate>,
        avklartUke: AvklartKjørtUke?,
        kjøreliste: Kjøreliste?,
        erUkeSlettet: Boolean,
    ): UkeVurderingDto =
        UkeVurderingDto(
            ukenummer = uke.ukenummer,
            fraDato = datoer.min(),
            tilDato = datoer.max(),
            erUkeSlettet = erUkeSlettet,
            status = avklartUke?.status ?: UkeStatus.IKKE_MOTTATT_KJØRELISTE,
            avvik = avklartUke?.typeAvvik?.let { AvvikUke(typeAvvik = it) },
            behandletDato = avklartUke?.behandletDato,
            kjørelisteInnsendtDato = kjøreliste?.datoMottatt?.toLocalDate(),
            kjørelisteId = kjøreliste?.id,
            avklartUkeId = avklartUke?.id,
            avklartKjørtUkeStatus = avklartUke?.avklartKjørtUkeStatus,
            dager =
                datoer.map { dato ->
                    lagDagDto(
                        dato = dato,
                        kjørelisteForUke = kjøreliste,
                        avklartUke = avklartUke,
                    )
                },
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
            avklartKjørtDagStatus = avklartKjørtDagStatus,
        )

    private fun erUkeSlettet(
        uke: UkeIÅr,
        gjeldendeUker: Map<UkeIÅr, List<LocalDate>>,
        forrigeUker: Map<UkeIÅr, List<LocalDate>>,
    ): Boolean {
        if (gjeldendeUker.isEmpty()) return true
        return !gjeldendeUker.containsKey(uke) && forrigeUker.containsKey(uke)
    }
}
