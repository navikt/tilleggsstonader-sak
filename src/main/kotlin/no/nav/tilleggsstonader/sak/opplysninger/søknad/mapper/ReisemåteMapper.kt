package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.søknad.felles.ReisemåteAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.DrosjeInfo
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.InfoBilKunDelerAvStrekning
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.LeveringOgHentingIBarnehage
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.OffentligTransportInfo
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.PrivatBilInfo
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.Reisemåte
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.UnntakFraOffentligTransport
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.UtgifterPrivatBil

object ReisemåteMapper {
    fun mapReisemåte(reisemåte: ReisemåteAvsnitt): Reisemåte =
        Reisemåte(
            hvilkeTransportmidlerBleBenyttet = reisemåte.hvilkeTransportmidlerBleBenyttet?.verdier?.map { it.verdi },
            unntakFraOffentligTransport =
                reisemåte.unntakFraOffentligTransport?.let { unntak ->
                    UnntakFraOffentligTransport(
                        årsaker = unntak.årsaker?.verdier?.map { it.verdi },
                        leveringOgHentingIBarnehage =
                            unntak.leveringOgHentingIBarnehage?.let {
                                LeveringOgHentingIBarnehage(
                                    gateadresse = it.gateadresse?.verdi,
                                    postnummer = it.postnummer?.verdi,
                                )
                            },
                    )
                },
            unntakFraPrivatBil = reisemåte.unntakFraPrivatBil?.verdier?.map { it.verdi },
            offentligTransport =
                reisemåte.offentligTransport?.let {
                    OffentligTransportInfo(
                        totalUtgifterOffentligTransport = it.totalUtgifterOffentligTransport?.verdi,
                    )
                },
            privatBil =
                reisemåte.privatBil?.let { privatBil ->
                    PrivatBilInfo(
                        benyttetEgenBil = privatBil.benyttetEgenBil?.verdi,
                        betalteForReisen = privatBil.betalteForReisen?.verdi,
                        infoBilKunDelerAvStrekning =
                            privatBil.infoBilKunDelerAvStrekning?.let {
                                InfoBilKunDelerAvStrekning(
                                    strekningHvorBilBleBenyttet = it.strekningHvorBilBleBenyttet?.verdi,
                                    antallKilometerKjørt = it.antallKilometerKjørt?.verdi,
                                )
                            },
                        utgifterPrivatBil =
                            privatBil.utgifterPrivatBil?.let {
                                UtgifterPrivatBil(
                                    bompenger = it.bompenger?.verdi,
                                    ferge = it.ferge?.verdi,
                                    piggdekkavgift = it.piggdekkavgift?.verdi,
                                    parkering = it.parkering?.verdi,
                                    drivstoffType = it.drivstoffType?.verdi,
                                )
                            },
                    )
                },
            drosje =
                reisemåte.drosje?.let {
                    DrosjeInfo(harTTKort = it.harTTKort?.verdi)
                },
        )
}
