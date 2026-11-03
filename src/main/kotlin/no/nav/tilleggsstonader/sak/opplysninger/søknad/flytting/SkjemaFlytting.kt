package no.nav.tilleggsstonader.sak.opplysninger.søknad.flytting

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttemåte
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flytteårsak
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.UtgifterDekketPåAnnenMåte
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Søknadsdata
import java.math.BigDecimal
import java.time.LocalDate

data class SkjemaFlytting(
    override val hovedytelse: HovedytelseAvsnitt,
    val flyttingDetaljer: FlyttingDetaljer?,
    val dokumentasjon: List<Dokumentasjon>,
) : Søknadsdata

data class FlyttingDetaljer(
    val årsak: Flytteårsak,
    val flyttedato: LocalDate,
    val adresse: Adresse,
    val flyttemåte: Flyttemåte,
    val utgifterDekketPåAnnenMåte: UtgifterDekketPåAnnenMåte,
    val utgifterEgenFlytting: UtgifterEgenFlytting?,
    val flyttetilbud: List<Flyttetilbud>?,
    val skalFlytteFraFolkeregistrertAdresse: JaNei,
    val adresseDetSkalFlyttesFra: Adresse,
)

data class UtgifterEgenFlytting(
    val avstand: BigDecimal,
    val hengerleie: BigDecimal?,
    val leiebil: BigDecimal?,
    val bom: BigDecimal?,
    val parkering: BigDecimal?,
    val ferje: BigDecimal?,
    val annet: BigDecimal?,
)

data class Flyttetilbud(
    val navn: String,
    val beløp: BigDecimal,
)
