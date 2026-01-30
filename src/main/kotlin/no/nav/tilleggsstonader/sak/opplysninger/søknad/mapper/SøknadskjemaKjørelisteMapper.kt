package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaKjøreliste
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadKjøreliste
import java.time.LocalDateTime

object SøknadskjemaKjørelisteMapper {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: KjørelisteSkjema,
    ): SøknadKjøreliste =
        SøknadKjøreliste(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data = mapSkjemaKjøreliste(skjema),
        )

    fun mapSkjemaKjøreliste(skjema: KjørelisteSkjema) =
        SkjemaKjøreliste(
            reiser = skjema.reisedagerPerUkeAvsnitt,
            dokumentasjon = skjema.dokumentasjon,
        )
}
