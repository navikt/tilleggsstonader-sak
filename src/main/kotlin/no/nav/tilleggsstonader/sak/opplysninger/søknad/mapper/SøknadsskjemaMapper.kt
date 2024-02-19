package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn

object SøknadsskjemaMapper {
    fun map(skjema: Søknadsskjema<SøknadsskjemaBarnetilsyn>, journalpostId: String): SøknadBarnetilsyn {
        return SøknadBarnetilsyn(
            journalpostId = journalpostId,
            mottattTidspunkt = skjema.mottattTidspunkt,
            språk = skjema.språk,
            data = mapSkjemaBarnetilsyn(skjema),
            barn = mapBarn(skjema),
        )
    }

    private fun mapSkjemaBarnetilsyn(skjema: Søknadsskjema<SøknadsskjemaBarnetilsyn>) =
        SkjemaBarnetilsyn(
            hovedytelse = HovedytelseAvsnitt(
                hovedytelse = skjema.skjema.hovedytelse.hovedytelse.verdier.map { it.verdi },
                boddSammenhengende = skjema.skjema.hovedytelse.boddSammenhengende?.verdi,
                planleggerBoINorgeNeste12mnd = skjema.skjema.hovedytelse.planleggerBoINorgeNeste12mnd?.verdi,
            ),
            aktivitet = AktivitetAvsnitt(
                utdanning = skjema.skjema.aktivitet.utdanning.verdi,
            ),
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
