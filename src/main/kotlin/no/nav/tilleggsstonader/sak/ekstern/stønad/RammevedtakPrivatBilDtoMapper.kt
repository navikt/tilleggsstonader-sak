package no.nav.tilleggsstonader.sak.ekstern.stønad

import io.github.mikaojk.holiday.getNorwegianHolidays
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.søknad.HelligdagDto
import no.nav.tilleggsstonader.kontrakter.søknad.RammevedtakDto
import no.nav.tilleggsstonader.kontrakter.søknad.RammevedtakUkeDto
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.util.erFørNåværendeUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

fun RammevedtakPrivatBil.tilDto(kjørelister: Map<ReiseId, List<Kjøreliste>>): List<RammevedtakDto> =
    reiser.map { reise ->
        val kjørelisterForReise = kjørelister[reise.reiseId] ?: emptyList()
        RammevedtakDto(
            reiseId = reise.reiseId.toString(),
            fom = reise.grunnlag.fom,
            tom = reise.grunnlag.tom,
            aktivitetsadresse = reise.aktivitetsadresse ?: "Ukjent adresse",
            aktivitetsnavn = reise.tiltaksvariant?.beskrivelse ?: reise.aktivitetType.name,
            uker =
                reise.grunnlag
                    .alleDatoerGruppertPåUke()
                    .map { (uke, datoer) ->
                        val kjørelisteForUke = kjørelisterForReise.finnForUke(uke)
                        RammevedtakUkeDto(
                            fom = datoer.min(),
                            tom = datoer.max(),
                            ukeNummer = uke.ukenummer,
                            reisedagerPerUke = reise.finnDelperiodeForPeriode(Datoperiode(datoer.min(), datoer.max())).reisedagerPerUke,
                            innsendtDato = kjørelisteForUke?.datoMottatt?.toLocalDate(),
                            kanSendeInnKjøreliste = uke.erFørNåværendeUke(),
                        )
                    },
            helligdager = finnHelligdagerInnenforPeriode(reise.grunnlag.fom, reise.grunnlag.tom),
        )
    }

fun List<Kjøreliste>.finnForUke(uke: UkeIÅr): Kjøreliste? = firstOrNull { it.inneholderUke(uke) }

private fun Kjøreliste.inneholderUke(uke: UkeIÅr): Boolean = data.reisedager.any { it.dato.tilUkeIÅr() == uke }

private fun finnHelligdagerInnenforPeriode(
    fom: LocalDate,
    tom: LocalDate,
): List<HelligdagDto> =
    (fom.year..tom.year)
        .flatMap { getNorwegianHolidays(it) }
        .filter { it.date in fom..tom }
        .map { HelligdagDto(dato = it.date, navn = it.name) }
