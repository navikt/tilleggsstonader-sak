package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
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
    ) = SkjemaBarnetilsyn(
        hovedytelse = hovedytelse,
        aktivitet = aktivitet,
    )

    fun lagAktivitet(
        utdanning: JaNei = JaNei.JA,
    ) = AktivitetAvsnitt(
        utdanning = utdanning,
    )

    private fun lagHovedytelse(
        hovedytelse: Hovedytelse = Hovedytelse.AAP,
    ) = HovedytelseAvsnitt(
        hovedytelse = hovedytelse,
    )
}
