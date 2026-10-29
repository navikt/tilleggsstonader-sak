package no.nav.tilleggsstonader.sak.opplysninger.søknad.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaFlytting
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadFlytting
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttingDetaljerAvsnitt as FlyttingDetaljerKontrakt

object SøknadsskjemaFlyttingMapper {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: SøknadsskjemaFlytting,
    ): SøknadFlytting =
        SøknadFlytting(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data =
                SkjemaFlytting(
                    hovedytelse =
                        HovedytelseAvsnitt(
                            hovedytelse =
                                skjema.hovedytelse.hovedytelse.verdier
                                    .map { it.verdi },
                            harNedsattArbeidsevne = null, // Finnes ikke i søknad ennå
                            arbeidOgOpphold = mapArbeidOgOpphold(skjema.hovedytelse.arbeidOgOpphold),
                        ),
                    flyttingDetaljer = skjema.flyttingDetaljer?.let { mapFlyttingDetaljer(it) },
                    dokumentasjon = mapDokumentasjon(skjema, journalpost),
                ),
        )

    private fun mapFlyttingDetaljer(detaljer: FlyttingDetaljerKontrakt) =
        FlyttingDetaljer(
            årsak = detaljer.årsak.verdi,
            flyttedato = LocalDate.parse(detaljer.flyttedato.verdi),
            adresse =
                detaljer.adresse.let {
                    Adresse(
                        gyldigFraOgMed = null,
                        adresse = it.gateadresse?.verdi,
                        postnummer = it.postnummer?.verdi,
                        poststed = it.poststed?.verdi,
                        landkode = it.land?.verdi,
                    )
                },
            flyttemåte = detaljer.flyttemåte.verdi,
            utgifterDekketPåAnnenMåte = detaljer.utgifterDekketPåAnnenMåte.verdi,
            skalFlytteFraFolkeregistrertAdresse = detaljer.skalFlytteFraFolkeregistrertAdresse.verdi,
            adresseDetSkalFlyttesFra =
                Adresse(
                    gyldigFraOgMed = null,
                    adresse = detaljer.adresseDetSkalFlyttesFra.gateadresse?.verdi,
                    postnummer = detaljer.adresseDetSkalFlyttesFra.postnummer?.verdi,
                    poststed = detaljer.adresseDetSkalFlyttesFra.poststed?.verdi,
                    landkode = detaljer.adresseDetSkalFlyttesFra.land?.verdi,
                ),
            utgifterEgenFlytting =
                detaljer.utgifterEgenFlytting?.let {
                    UtgifterEgenFlytting(
                        avstand = it.avstand.verdi,
                        hengerleie = it.hengerleie?.verdi,
                        leiebil = it.leiebil?.verdi,
                        bom = it.bom?.verdi,
                        parkering = it.parkering?.verdi,
                        ferje = it.ferje?.verdi,
                        annet = it.annet?.verdi,
                    )
                },
            flyttetilbud =
                detaljer.flyttebyrå?.tilbud?.map {
                    Flyttetilbud(
                        navn = it.navn.verdi,
                        beløp = it.beløp.verdi,
                    )
                },
        )
}
