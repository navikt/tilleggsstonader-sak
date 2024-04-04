package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokument
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import java.time.LocalDate
import java.time.LocalDateTime

object SøknadBarnetilsynUtil {

    fun søknadBarnetilsyn(
        data: SkjemaBarnetilsyn = lagSkjemaBarnetilsyn(),
        barn: Set<SøknadBarn> = setOf(
            lagSøknadBarn(),
        ),

        journalpostId: String = "testId",
        språk: Språkkode = Språkkode.NB,
        mottattTidspunkt: LocalDateTime = LocalDate.of(2023, 1, 1).atStartOfDay(),
    ) =
        SøknadBarnetilsyn(
            journalpostId = journalpostId,
            språk = språk,
            mottattTidspunkt = mottattTidspunkt,
            data = data,
            barn = barn,
        )

    fun lagSøknadBarn(
        ident: String = "1",
        data: BarnMedBarnepass = lagBarnMedBarnepass(),
    ) = SøknadBarn(
        ident = ident,
        data = data,
    )

    fun lagBarnMedBarnepass(
        type: TypeBarnepass = TypeBarnepass.ANDRE,
        startetIFemte: JaNei = JaNei.JA,
        årsak: ÅrsakBarnepass = ÅrsakBarnepass.MYE_BORTE_ELLER_UVANLIG_ARBEIDSTID,
    ) = BarnMedBarnepass(
        type = type,
        startetIFemte = startetIFemte,
        årsak = årsak,
    )

    fun lagSkjemaBarnetilsyn(
        hovedytelse: HovedytelseAvsnitt = lagHovedytelse(),
        aktivitet: AktivitetAvsnitt = lagAktivitet(),
        dokumentasjon: List<Dokumentasjon> = listOf(lagDokumentasjon()),
    ) = SkjemaBarnetilsyn(
        hovedytelse = hovedytelse,
        aktivitet = aktivitet,
        dokumentasjon = dokumentasjon,
    )

    fun lagDokumentasjon(
        identBarn: String? = null,
    ): Dokumentasjon = Dokumentasjon(
        type = Vedleggstype.UTGIFTER_PASS_ANNET,
        dokumenter = listOf(
            Dokument("688ad1dc-e35e-4ab8-a534-17c6e691463f"),
        ),
        identBarn = identBarn,
    )

    fun lagAktivitet(
        utdanning: JaNei = JaNei.JA,
    ) = AktivitetAvsnitt(
        utdanning = utdanning,
    )

    private fun lagHovedytelse(
        vararg hovedytelse: Hovedytelse = arrayOf(Hovedytelse.AAP),
    ) = HovedytelseAvsnitt(
        hovedytelse = hovedytelse.toList(),
        arbeidOgOpphold = arbeidOgOpphold(),
    )

    private fun arbeidOgOpphold() = ArbeidOgOpphold(
        jobberIAnnetLand = JaNei.JA,
        jobbAnnetLand = "Sverige",
        harPengestøtteAnnetLand = listOf(TypePengestøtte.SYKEPENGER),
        pengestøtteAnnetLand = "Sverige",
        harOppholdUtenforNorgeSiste12mnd = JaNei.JA,
        oppholdUtenforNorgeSiste12mnd = listOf(oppholdUtenforNorge()),
        harOppholdUtenforNorgeNeste12mnd = JaNei.JA,
        oppholdUtenforNorgeNeste12mnd = listOf(oppholdUtenforNorge()),
    )

    private fun oppholdUtenforNorge() = OppholdUtenforNorge(
        land = "Sverige",
        årsak = listOf(ÅrsakOppholdUtenforNorge.JOBB),
        fom = LocalDate.of(2024, 1, 1),
        tom = LocalDate.of(2024, 1, 1),
    )
}
