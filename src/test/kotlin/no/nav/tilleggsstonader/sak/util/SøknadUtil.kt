package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Dokument
import no.nav.tilleggsstonader.kontrakter.søknad.DokumentasjonFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.TekstFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ArbeidOgOpphold
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnMedBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.OppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

object SøknadUtil {

    fun søknadskjemaBarnetilsyn(
        ident: String = "søker",
        mottattTidspunkt: LocalDateTime = osloNow(),
        barnMedBarnepass: List<BarnMedBarnepass> = listOf(barnMedBarnepass()),
        dokumentasjon: List<DokumentasjonFelt> = emptyList(),
    ): Søknadsskjema<SøknadsskjemaBarnetilsyn> {
        val skjemaBarnetilsyn = SøknadsskjemaBarnetilsyn(
            hovedytelse = HovedytelseAvsnitt(
                hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "")), emptyList()),
                arbeidOgOpphold = arbeidOgOpphold(),
            ),
            aktivitet = AktivitetAvsnitt(
                aktiviteter = EnumFlereValgFelt(
                    "Hvilken aktivitet søker du om støtte i forbindelse med?",
                    listOf(VerdiFelt("ANNET", "Annet")),
                    listOf(),
                ),
                annenAktivitet = EnumFelt(
                    "Hvilken arbeidsrettet aktivitet har du? ",
                    AnnenAktivitetType.TILTAK,
                    "Tiltak / arbeidsrettet aktivitet",
                    listOf(),
                ),
                lønnetAktivitet = EnumFelt("Mottar du lønn gjennom ett tiltak?", JaNei.NEI, "Nei", listOf()),
            ),
            barn = BarnAvsnitt(barnMedBarnepass = barnMedBarnepass),
            dokumentasjon = dokumentasjon,
        )
        return Søknadsskjema(
            ident = ident,
            mottattTidspunkt = mottattTidspunkt,
            språk = Språkkode.NB,
            skjema = skjemaBarnetilsyn,
        )
    }

    private fun arbeidOgOpphold() = ArbeidOgOpphold(
        jobberIAnnetLand = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
        jobbAnnetLand = SelectFelt("Hvilket land jobber du i?", "SWE", "Sverige"),
        harPengestøtteAnnetLand = EnumFlereValgFelt(
            "Mottar du pengestøtte fra et annet land enn Norge?",
            listOf(
                VerdiFelt(
                    TypePengestøtte.SYKEPENGER,
                    "Sykepenger",
                ),
            ),
            emptyList(),
        ),
        pengestøtteAnnetLand = SelectFelt("Hvilket land mottar du pengestøtte fra?", "SWE", "Sverige"),
        harOppholdUtenforNorgeSiste12mnd = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
        oppholdUtenforNorgeSiste12mnd = listOf(oppholdUtenforNorge()),
        harOppholdUtenforNorgeNeste12mnd = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
        oppholdUtenforNorgeNeste12mnd = listOf(oppholdUtenforNorge()),
    )

    private fun oppholdUtenforNorge() = OppholdUtenforNorge(
        land = SelectFelt("Hvilket land har du oppholdt deg i?", "SWE", "Sverige"),
        årsak = EnumFlereValgFelt(
            "Hva gjorde du i dette landet?",
            listOf(VerdiFelt(ÅrsakOppholdUtenforNorge.JOBB, "Jobb")),
            alternativer = emptyList(),
        ),
        fom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
        tom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
    )

    fun lagDokumentasjon(
        type: Vedleggstype = Vedleggstype.UTGIFTER_PASS_SFO_AKS_BARNEHAGE,
        label: String = "Label vedlegg",
        vedlegg: List<Dokument> = emptyList(),
        barnId: String? = null,
    ): DokumentasjonFelt = DokumentasjonFelt(
        type = type,
        label = label,
        opplastedeVedlegg = vedlegg,
        barnId = barnId,
    )

    fun barnMedBarnepass(
        ident: String = FnrGenerator.generer(
            Year.now().minusYears(1).value,
            5,
            19,
        ),
        navn: String = "navn",
    ): BarnMedBarnepass = BarnMedBarnepass(
        ident = TekstFelt("", ident),
        navn = TekstFelt("", navn),
        type = EnumFelt("", TypeBarnepass.BARNEHAGE_SFO_AKS, "", emptyList()),
        startetIFemte = EnumFelt("", JaNei.JA, "", emptyList()),
        årsak = EnumFelt("", ÅrsakBarnepass.MYE_BORTE_ELLER_UVANLIG_ARBEIDSTID, "", emptyList()),
    )
}
