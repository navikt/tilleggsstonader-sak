package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.TekstFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnMedBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.SøknadsskjemaBarnetilsyn
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
    ): Søknadsskjema<SøknadsskjemaBarnetilsyn> {
        val skjemaBarnetilsyn = SøknadsskjemaBarnetilsyn(
            hovedytelse = HovedytelseAvsnitt(
                hovedytelse = EnumFelt("", Hovedytelse.AAP, "", emptyList()),
            ),
            aktivitet = AktivitetAvsnitt(
                utdanning = EnumFelt("", JaNei.JA, "", emptyList()),
            ),
            barn = BarnAvsnitt(barnMedBarnepass = barnMedBarnepass),
            dokumentasjon = emptyList(),
        )
        return Søknadsskjema(
            ident = ident,
            mottattTidspunkt = mottattTidspunkt,
            språk = Språkkode.NB,
            skjema = skjemaBarnetilsyn,
        )
    }

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
