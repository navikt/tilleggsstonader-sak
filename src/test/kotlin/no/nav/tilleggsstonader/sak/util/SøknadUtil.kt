package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.Dokument
import no.nav.tilleggsstonader.kontrakter.søknad.DokumentasjonFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.TekstFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnMedBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import java.time.LocalDateTime
import java.time.Year

object SøknadUtil {

    fun søknadskjemaBarnetilsyn(
        ident: String = "søker",
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
        barnMedBarnepass: List<BarnMedBarnepass> = listOf(barnMedBarnepass()),
        dokumentasjon: List<DokumentasjonFelt> = emptyList(),
    ): Søknadsskjema<SøknadsskjemaBarnetilsyn> {
        val skjemaBarnetilsyn = SøknadsskjemaBarnetilsyn(
            hovedytelse = HovedytelseAvsnitt(
                hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "")), emptyList()),
                boddSammenhengende = EnumFelt(
                    "Har du bodd sammenhengende i Norge de siste 12 månedene?",
                    JaNei.JA,
                    "Ja",
                    emptyList(),
                ),
                planleggerBoINorgeNeste12mnd = EnumFelt(
                    "Planlegger du å bo i Norge de neste 12 månedene?",
                    JaNei.JA,
                    "Ja",
                    emptyList(),
                ),
            ),
            aktivitet = AktivitetAvsnitt(
                utdanning = EnumFelt("", JaNei.JA, "", emptyList()),
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
