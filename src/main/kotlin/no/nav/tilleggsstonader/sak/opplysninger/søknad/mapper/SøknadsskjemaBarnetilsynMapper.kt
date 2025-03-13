package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Utgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon
import java.time.LocalDateTime

object SøknadsskjemaBarnetilsynMapper {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: SøknadsskjemaBarnetilsyn,
    ): SøknadBarnetilsyn =
        SøknadBarnetilsyn(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data = mapSkjemaBarnetilsyn(skjema, journalpost),
            barn = mapBarn(skjema),
        )

    private fun mapSkjemaBarnetilsyn(
        skjema: SøknadsskjemaBarnetilsyn,
        journalpost: Journalpost,
    ) = SkjemaBarnetilsyn(
        hovedytelse =
            HovedytelseAvsnitt(
                hovedytelse =
                    skjema.hovedytelse.hovedytelse.verdier
                        .map { it.verdi },
                arbeidOgOpphold = mapArbeidOgOpphold(skjema.hovedytelse.arbeidOgOpphold),
            ),
        aktivitet =
            AktivitetAvsnitt(
                aktiviteter =
                    skjema.aktivitet.aktiviteter
                        ?.verdier
                        ?.map { ValgtAktivitet(id = it.verdi, label = it.label) },
                annenAktivitet = skjema.aktivitet.annenAktivitet?.verdi,
                lønnetAktivitet = skjema.aktivitet.lønnetAktivitet?.verdi,
            ),
        dokumentasjon = mapDokumentasjon(skjema, journalpost),
    )

    private fun mapBarn(skjema: SøknadsskjemaBarnetilsyn) =
        skjema.barn.barnMedBarnepass
            .map {
                SøknadBarn(
                    ident = it.ident.verdi,
                    data =
                        BarnMedBarnepass(
                            type = it.type.verdi,
                            utgifter =
                                it.utgifter?.let { utgifter ->
                                    Utgifter(
                                        harUtgifterTilPass = utgifter.harUtgifterTilPass.verdi,
                                        fom = utgifter.fom?.verdi,
                                        tom = utgifter.tom?.verdi,
                                    )
                                },
                            startetIFemte = it.startetIFemte?.verdi,
                            årsak = it.årsak?.verdi,
                        ),
                )
            }.toSet()
}
