package no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon
import java.time.LocalDateTime

object SøknadsskjemaReiseTilSamlingMapper {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: SøknadsskjemaReiseTilSamling,
    ): SøknadReiseTilSamling =
        SøknadReiseTilSamling(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data = mapSkjemaReiseTilSamling(skjema, journalpost),
        )

    private fun mapSkjemaReiseTilSamling(
        skjema: SøknadsskjemaReiseTilSamling,
        journalpost: Journalpost,
    ) = SkjemaReiseTilSamling(
        hovedytelse =
            HovedytelseAvsnitt(
                hovedytelse =
                    skjema.hovedytelse.hovedytelse.verdier
                        .map { it.verdi },
                harNedsattArbeidsevne = null, // Finnes ikke i søknad ennå
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
        samlinger =
            skjema.samlinger.mapNotNull { samling ->
                val fom = samling.fom?.verdi ?: return@mapNotNull null
                val tom = samling.tom?.verdi ?: return@mapNotNull null
                SamlingPeriode(fom = fom, tom = tom)
            },
        reiseavstand =
            Reiseavstand(
                antallKilometerEnVei = skjema.reiseavstand.antallKilometerEnVei?.verdi,
                land =
                    skjema.reiseavstand.aktivitetsadresse.land
                        ?.verdi,
                gateadresse =
                    skjema.reiseavstand.aktivitetsadresse.gateadresse
                        ?.verdi,
                postnummer =
                    skjema.reiseavstand.aktivitetsadresse.postnummer
                        ?.verdi,
                poststed =
                    skjema.reiseavstand.aktivitetsadresse.poststed
                        ?.verdi,
            ),
        reisemåte =
            Reisemåte(
                kanReiseKollektivt = skjema.reisemåte.kanReiseKollektivt?.verdi,
                totalutgifterKollektivt = skjema.reisemåte.totalutgifterKollektivt?.verdi,
                kanBenytteEgenBil = skjema.reisemåte.kanBenytteEgenBil?.verdi,
                kanBenytteDrosje = skjema.reisemåte.kanBenytteDrosje?.verdi,
            ),
        dokumentasjon = mapDokumentasjon(skjema, journalpost),
    )
}
