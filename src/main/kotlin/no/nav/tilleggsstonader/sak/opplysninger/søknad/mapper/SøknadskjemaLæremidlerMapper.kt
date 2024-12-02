package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaLæremidler
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadLæremidler
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.UtdanningAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon
import java.time.LocalDateTime

object SøknadskjemaLæremidlerMapper {
    fun map(mottattTidspunkt: LocalDateTime, språk: Språkkode, journalpost: Journalpost, skjema: SøknadsskjemaLæremidler): SøknadLæremidler {
        return SøknadLæremidler(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data = mapSkjemaLæremidler(skjema, journalpost),
        )
    }

    private fun mapSkjemaLæremidler(skjema: SøknadsskjemaLæremidler, journalpost: Journalpost) =
        SkjemaLæremidler(
            hovedytelse = HovedytelseAvsnitt(
                hovedytelse = skjema.hovedytelse.hovedytelse.verdier.map { it.verdi },
                arbeidOgOpphold = mapArbeidOgOpphold(skjema.hovedytelse.arbeidOgOpphold),
            ),
            utdanning = UtdanningAvsnitt(
                aktiviteter = skjema.utdanning.aktiviteter?.verdier?.map { ValgtAktivitet(id = it.verdi, label = it.label) },
                annenUtdanning = skjema.utdanning.annenUtdanning?.verdi,
                erLærlingEllerLiknende = skjema.utdanning.erLærlingEllerLiknende?.verdi,
                harFunksjonsnedsettelse = skjema.utdanning.harFunksjonsnedsettelse.verdi,
            ),
            dokumentasjon = mapDokumentasjon(skjema, journalpost),
        )
}
