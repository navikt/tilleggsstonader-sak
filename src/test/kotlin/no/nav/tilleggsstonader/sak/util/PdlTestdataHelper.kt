package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Bostedsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Dødsfall
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisteridentifikator
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.FolkeregisteridentifikatorStatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregistermetadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.ForelderBarnRelasjon
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Fødselsdato
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.IdentifiserendeInformasjon
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.InnflyttingTilNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Kontaktadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.KontaktinformasjonForDoedsbo
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.KontaktinformasjonForDoedsboAdresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.KontaktinformasjonForDoedsboSkifteform
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Metadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Opphold
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Oppholdsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Personnavn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Statsborgerskap
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UkjentBosted
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.UtflyttingFraNorge
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.VergeEllerFullmektig
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

object PdlTestdataHelper {
    val metadataGjeldende = Metadata(historisk = false)
    val metadataHistorisk = Metadata(historisk = true)

    fun lagNavn(
        fornavn: String = "Fornavn",
        mellomnavn: String? = "mellomnavn",
        etternavn: String = "Etternavn",
        historisk: Boolean = false,
    ): Navn =
        Navn(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            metadata = Metadata(historisk = historisk),
        )

    fun pdlSøker(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        bostedsadresse: List<Bostedsadresse> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
        fødselsdato: List<Fødselsdato> = listOf(fødsel(år = 2000)),
        folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = emptyList(),
        kontaktadresse: List<Kontaktadresse> = emptyList(),
        kontaktinformasjonForDoedsbo: List<KontaktinformasjonForDoedsbo> = emptyList(),
        navn: List<Navn> = listOf(lagNavn()),
        opphold: List<Opphold> = emptyList(),
        oppholdsadresse: List<Oppholdsadresse> = emptyList(),
        statsborgerskap: List<Statsborgerskap> = emptyList(),
        innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
        utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList(),
        vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt> = emptyList(),
        folkeregisteridentifikator: List<Folkeregisteridentifikator> = emptyList(),
    ) = PdlSøker(
        adressebeskyttelse = adressebeskyttelse,
        bostedsadresse = bostedsadresse,
        dødsfall = dødsfall,
        forelderBarnRelasjon = forelderBarnRelasjon,
        folkeregisteridentifikator = folkeregisteridentifikator,
        fødselsdato = fødselsdato,
        folkeregisterpersonstatus = folkeregisterpersonstatus,
        kontaktadresse = kontaktadresse,
        kontaktinformasjonForDoedsbo = kontaktinformasjonForDoedsbo,
        navn = navn,
        opphold = opphold,
        oppholdsadresse = oppholdsadresse,
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
        fødselsdato: Fødselsdato? =
            Fødselsdato(
                fødselsår = 2025,
                fødselsdato = LocalDate.of(2025, 1, 1),
                metadata = metadataGjeldende,
            ),
        navn: Navn = lagNavn(),
    ) = PdlBarn(
        adressebeskyttelse = adressebeskyttelse,
        bostedsadresse = bostedsadresse,
        dødsfall = dødsfall,
        forelderBarnRelasjon = forelderBarnRelasjon,
        fødselsdato = listOfNotNull(fødselsdato),
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

    fun fødsel(
        år: Int = 2000,
        måned: Int = 1,
        dag: Int = 1,
    ): Fødselsdato = fødsel(LocalDate.of(år, måned, dag))

    fun fødsel(fødselsdato: LocalDate) =
        Fødselsdato(
            fødselsår = fødselsdato.year,
            fødselsdato = fødselsdato,
            metadata = metadataGjeldende,
        )

    fun ukjentBostedsadresse(
        bostedskommune: String = "1234",
        historisk: Boolean = false,
    ) = Bostedsadresse(
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

    fun vergemaalEllerFremtidsfullmakt(
        embete: String? = null,
        folkeregistermetadata: Folkeregistermetadata? = null,
        type: String? = "voksen",
        vergeEllerFullmektig: VergeEllerFullmektig = vergeEllerFullmektig(),
    ) = VergemaalEllerFremtidsfullmakt(
        embete = embete,
        folkeregistermetadata = folkeregistermetadata,
        type = type,
        vergeEllerFullmektig = vergeEllerFullmektig,
    )

    fun vergeEllerFullmektig(
        motpartsPersonident: String? = "1",
        navn: Personnavn? = null,
        omfang: String? = "personligeOgOekonomiskeInteresser",
    ) = VergeEllerFullmektig(
        motpartsPersonident = motpartsPersonident,
        identifiserendeInformasjon = IdentifiserendeInformasjon(navn = navn),
        omfang = omfang,
    )

    fun folkeregistermetadata() =
        Folkeregistermetadata(
            gyldighetstidspunkt = java.time.LocalDateTime.now(),
            opphørstidspunkt = null,
        )

    fun kontaktinformasjonDødsbo() =
        KontaktinformasjonForDoedsbo(
            adresse =
                KontaktinformasjonForDoedsboAdresse(
                    adresselinje1 = "Dødsbogate 1",
                    adresselinje2 = null,
                    landkode = "NO",
                    postnummer = "0123",
                    poststedsnavn = "OSLO",
                ),
            advokatSomKontakt = null,
            attestutstedelsesdato = LocalDate.now(),
            folkeregistermetadata = folkeregistermetadata(),
            metadata = metadataGjeldende,
            organisasjonSomKontakt = null,
            personSomKontakt = null,
            skifteform = KontaktinformasjonForDoedsboSkifteform.OFFENTLIG,
        )
}
