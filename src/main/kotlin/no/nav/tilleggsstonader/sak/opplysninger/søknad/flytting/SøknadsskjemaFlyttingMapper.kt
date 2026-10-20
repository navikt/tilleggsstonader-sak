package no.nav.tilleggsstonader.sak.opplysninger.søknad.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaFlytting
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadFlytting
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon
import java.time.LocalDateTime

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
                            // TODO Hvordan gjøre det med aktivitetspenger her?
                            hovedytelse = listOf(Hovedytelse.AKTIVITETSPENGER),
                            harNedsattArbeidsevne = null,
                            arbeidOgOpphold = null,
                        ),
                    flyttingDetaljer = skjema.flyttingDetaljer,
                    dokumentasjon = mapDokumentasjon(skjema, journalpost),
                ),
        )
}
