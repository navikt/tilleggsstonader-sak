package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager.RegistrertKjørtUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

object ReisevurderingPrivatBilMapper {
    fun tilReisevurderingDto(
        gjeldendeRammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
        forrigeRammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
        avklarteUker: List<AvklartKjørtUke>,
        kjørelister: List<Kjøreliste>,
        registrerteUker: List<RegistrertKjørtUke> = emptyList(),
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
                registrerteUker = registrerteUker,
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
        gjeldendeRammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
        forrigeRammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
    ): ReiseId =
        gjeldendeRammevedtakForReise?.reiseId ?: forrigeRammevedtakForReise?.reiseId
        ?: feil("Kan ikke lage reisevudering. Mangler rammevedtak for reise")

    private fun lagUkeVurderingerDto(
        gjeldendeRammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
        forrigeRammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
        avklarteUker: List<AvklartKjørtUke>,
        kjørelister: List<Kjøreliste>,
        registrerteUker: List<RegistrertKjørtUke>,
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
            val gjeldendeDatoerForUke = gjeldendeUker[uke].orEmpty()
            val datoerForUke = (gjeldendeDatoerForUke + forrigeUker[uke].orEmpty()).distinct().sorted()
            val avklartUke = avklarteUker.singleOrNull { it.reiseId == reiseId && it.uke == uke }
            val kjørelisteForUke = avklartUke?.let { kjørelister.firstOrNull { it.id == avklartUke.kjørelisteId } }
            val registrertKjørtUke =
                registrerteUker.singleOrNull { it.reiseId == reiseId && it.dager.any { dag -> dag.dato in datoerForUke } }

            lagUkeVurderingDto(
                uke = uke,
                datoer = datoerForUke,
                gjeldendeDatoerForUke = gjeldendeDatoerForUke,
                avklartUke = avklartUke,
                kjøreliste = kjørelisteForUke,
                erUkeSlettet = erUkeSlettet(uke, gjeldendeUker, forrigeUker),
                registrertKjørtUke = registrertKjørtUke,
            )
        }
    }

    fun lagUkeVurderingDto(
        uke: UkeIÅr,
        datoer: List<LocalDate>,
        gjeldendeDatoerForUke: List<LocalDate>,
        avklartUke: AvklartKjørtUke?,
        kjøreliste: Kjøreliste?,
        erUkeSlettet: Boolean,
        registrertKjørtUke: RegistrertKjørtUke? = null,
    ): UkeVurderingDto =
        UkeVurderingDto(
            ukenummer = uke.ukenummer,
            fraDato = datoer.min(),
            tilDato = datoer.max(),
            erUkeSlettet = erUkeSlettet,
            status =
                avklartUke?.status
                    ?: if (registrertKjørtUke != null) UkeStatus.MANUELT_REGISTRERT else UkeStatus.IKKE_MOTTATT_KJØRELISTE,
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
                        gjeldendeDatoerForUke = gjeldendeDatoerForUke,
                        kjørelisteForUke = kjøreliste,
                        avklartUke = avklartUke,
                        registrertKjørtUke = registrertKjørtUke,
                    )
                },
        )

    private fun lagDagDto(
        dato: LocalDate,
        gjeldendeDatoerForUke: List<LocalDate>,
        kjørelisteForUke: Kjøreliste?,
        avklartUke: AvklartKjørtUke?,
        registrertKjørtUke: RegistrertKjørtUke? = null,
    ): DagDto =
        DagDto(
            dato = dato,
            ukedag = dato.dayOfWeek.name,
            erDagSlettet = !gjeldendeDatoerForUke.contains(dato),
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
                    }
                    ?: registrertKjørtUke
                        ?.dager
                        ?.firstOrNull { it.dato == dato }
                        ?.let { registrertDag ->
                            KjørelisteDagDto(
                                harKjørt = registrertDag.harKjørt,
                                parkeringsutgift = registrertDag.parkeringsutgift,
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
    ): Boolean = !gjeldendeUker.containsKey(uke) && forrigeUker.containsKey(uke)
}
