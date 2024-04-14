package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Bostedsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Dødsfall
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisteridentifikator
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.FolkeregisteridentifikatorStatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.ForelderBarnRelasjon
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Fullmakt
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Fødsel
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.InnflyttingTilNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Kjønn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.KjønnType
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Kontaktadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Metadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Opphold
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Oppholdsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonForelderBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Sivilstand
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Statsborgerskap
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UkjentBosted
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UtflyttingFraNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

object PdlTestdataHelper {

    val metadataGjeldende = Metadata(historisk = false)
    val metadataHistorisk = Metadata(historisk = true)

    fun lagKjønn(kjønnType: KjønnType = KjønnType.KVINNE) = Kjønn(kjønnType)

    fun lagNavn(
        fornavn: String = "Fornavn",
        mellomnavn: String? = "mellomnavn",
        etternavn: String = "Etternavn",
        historisk: Boolean = false,
    ): Navn {
        return Navn(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            metadata = Metadata(historisk = historisk),
        )
    }

    fun pdlSøker(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        bostedsadresse: List<Bostedsadresse> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
        fødsel: List<Fødsel> = emptyList(),
        folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = emptyList(),
        fullmakt: List<Fullmakt> = emptyList(),
        kjønn: Kjønn? = null,
        kontaktadresse: List<Kontaktadresse> = emptyList(),
        navn: List<Navn> = listOf(lagNavn()),
        opphold: List<Opphold> = emptyList(),
        oppholdsadresse: List<Oppholdsadresse> = emptyList(),
        sivilstand: List<Sivilstand> = emptyList(),
        statsborgerskap: List<Statsborgerskap> = emptyList(),
        innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
        utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList(),
        vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt> = emptyList(),
        folkeregisteridentifikator: List<Folkeregisteridentifikator> = emptyList(),
    ) =
        PdlSøker(
            adressebeskyttelse = adressebeskyttelse,
            bostedsadresse = bostedsadresse,
            dødsfall = dødsfall,
            forelderBarnRelasjon = forelderBarnRelasjon,
            folkeregisteridentifikator = folkeregisteridentifikator,
            fødsel = fødsel,
            folkeregisterpersonstatus = folkeregisterpersonstatus,
            fullmakt = fullmakt,
            kjønn = listOfNotNull(kjønn),
            kontaktadresse = kontaktadresse,
            navn = navn,
            opphold = opphold,
            oppholdsadresse = oppholdsadresse,
            sivilstand = sivilstand,
            statsborgerskap = statsborgerskap,
            innflyttingTilNorge = innflyttingTilNorge,
            utflyttingFraNorge = utflyttingFraNorge,
            vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt,
        )

    fun pdlBarn(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        bostedsadresse: List<Bostedsadresse> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
        fødsel: Fødsel? = null,
        navn: Navn = lagNavn(),
    ) =
        PdlPersonForelderBarn(
            adressebeskyttelse = adressebeskyttelse,
            bostedsadresse = bostedsadresse,
            dødsfall = dødsfall,
            forelderBarnRelasjon = forelderBarnRelasjon,
            fødsel = listOfNotNull(fødsel),
            navn = listOfNotNull(navn),
        )

    fun pdlPersonKort(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        navn: Navn = lagNavn(),
        dødsfall: List<Dødsfall> = emptyList(),
    ) = PdlPersonKort(
        adressebeskyttelse = adressebeskyttelse,
        navn = listOfNotNull(navn),
        dødsfall = dødsfall,
    )

    fun fødsel(år: Int = 2018, måned: Int = 1, dag: Int = 1): Fødsel =
        fødsel(LocalDate.of(år, måned, dag))

    fun fødsel(fødselsdato: LocalDate) =
        Fødsel(
            fødselsår = fødselsdato.year,
            fødselsdato = fødselsdato,
            metadata = metadataGjeldende,
            fødested = null,
            fødekommune = null,
            fødeland = null,
        )

    fun ukjentBostedsadresse(
        bostedskommune: String = "1234",
        historisk: Boolean = false,
    ) =
        Bostedsadresse(
            angittFlyttedato = null,
            gyldigFraOgMed = null,
            gyldigTilOgMed = null,
            coAdressenavn = null,
            utenlandskAdresse = null,
            vegadresse = null,
            ukjentBosted = UkjentBosted(bostedskommune),
            matrikkeladresse = null,
            metadata = Metadata(historisk),
        )

    fun folkeregisteridentifikator(
        ident: String,
        status: FolkeregisteridentifikatorStatus = FolkeregisteridentifikatorStatus.I_BRUK,
        gjeldende: Boolean = true,
    ) = Folkeregisteridentifikator(
        ident = ident,
        status = status,
        metadata = if (gjeldende) metadataGjeldende else metadataHistorisk,
    )
}
