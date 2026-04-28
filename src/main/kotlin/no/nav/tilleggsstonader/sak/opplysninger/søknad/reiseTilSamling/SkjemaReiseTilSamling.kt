package no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import java.time.LocalDate

data class SkjemaReiseTilSamling(
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetAvsnitt,
    val samlinger: List<SamlingPeriode>,
    val oppmøteadresse: Adresse?,
    val kanReiseKollektivt: JaNei?,
    val totalbeløpKollektivt: Int?,
    val årsakIkkeKollektivt: SøknadsskjemaReiseTilSamling.ÅrsakIkkeKollektivt?,
    val kanBenytteEgenBil: JaNei?,
    val årsakIkkeEgenBil: SøknadsskjemaReiseTilSamling.ÅrsakIkkeEgenBil?,
    val kanBenytteDrosje: JaNei?,
    val dokumentasjon: List<Dokumentasjon>,
)

data class SamlingPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)
