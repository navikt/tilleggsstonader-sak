package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon

object SøknadsskjemaBarnetilsynMapper {
    fun map(skjema: Søknadsskjema<SøknadsskjemaBarnetilsyn>, journalpost: Journalpost): SøknadBarnetilsyn {
        return SøknadBarnetilsyn(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = skjema.mottattTidspunkt,
            språk = skjema.språk,
            data = mapSkjemaBarnetilsyn(skjema.skjema, journalpost),
            barn = mapBarn(skjema),
        )
    }

    private fun mapSkjemaBarnetilsyn(skjema: SøknadsskjemaBarnetilsyn, journalpost: Journalpost) =
        SkjemaBarnetilsyn(
            hovedytelse = HovedytelseAvsnitt(
                hovedytelse = skjema.hovedytelse.hovedytelse.verdier.map { it.verdi },
                arbeidOgOpphold = mapArbeidOgOpphold(skjema.hovedytelse.arbeidOgOpphold),
            ),
            aktivitet = AktivitetAvsnitt(
                utdanning = skjema.aktivitet.utdanning.verdi,
            ),
            dokumentasjon = mapDokumentasjon(skjema, journalpost),
        )

    private fun mapBarn(skjema: Søknadsskjema<SøknadsskjemaBarnetilsyn>) =
        skjema.skjema.barn.barnMedBarnepass.map {
            SøknadBarn(
                ident = it.ident.verdi,
                data = BarnMedBarnepass(
                    type = it.type.verdi,
                    startetIFemte = it.startetIFemte?.verdi,
                    årsak = it.årsak?.verdi,
                ),
            )
        }.toSet()
}
