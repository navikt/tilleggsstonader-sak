package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.BoligEllerOvernattingAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.DelerUtgifterFlereStederType
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.FasteUtgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.UtgifterFlereSteder
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.UtgifterIForbindelseMedSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.UtgifterNyBolig
import java.time.LocalDate

data class FaktaBoligEllerOvernatting(
    val søknadsgrunnlag: FaktaBoligEllerOvernattingSøknadsgrunnlag?,
)

data class FaktaBoligEllerOvernattingSøknadsgrunnlag(
    val fasteUtgifter: FaktaFasteUtgifter?,
    val samling: FaktaUtgifterIForbindelseMedSamling?,
    val harSærligStoreUtgifterPgaFunksjonsnedsettelse: JaNei,
)

data class FaktaFasteUtgifter(
    val utgifterFlereSteder: FaktaUtgifterFlereSteder?,
    val utgifterNyBolig: FaktaUtgifterNyBolig?,
)

data class FaktaUtgifterFlereSteder(
    val delerBoutgifter: List<DelerUtgifterFlereStederType>,
    val andelUtgifterBoligHjemsted: Int,
    val andelUtgifterBoligAktivitetssted: Int,
)

data class FaktaUtgifterIForbindelseMedSamling(
    val periodeForSamling: List<FaktaPeriodeForSamling>,
)

data class FaktaPeriodeForSamling(
    val fom: LocalDate,
    val tom: LocalDate,
    val trengteEkstraOvernatting: JaNei,
    val utgifterTilOvernatting: Int,
)

data class FaktaUtgifterNyBolig(
    val delerBoutgifter: JaNei,
    val andelUtgifterBolig: Int?,
    val harHoyereUtgifterPaNyttBosted: JaNei,
    val mottarBostotte: JaNei?,
)

fun BoligEllerOvernattingAvsnitt.tilFakta() =
    FaktaBoligEllerOvernattingSøknadsgrunnlag(
        fasteUtgifter = this.fasteUtgifter?.tilFakta(),
        samling = this.samling?.tilFakta(),
        harSærligStoreUtgifterPgaFunksjonsnedsettelse = this.harSærligStoreUtgifterPgaFunksjonsnedsettelse,
    )

private fun FasteUtgifter.tilFakta() =
    FaktaFasteUtgifter(
        utgifterFlereSteder = this.utgifterFlereSteder?.tilFakta(),
        utgifterNyBolig = this.utgifterNyBolig?.tilFakta(),
    )

private fun UtgifterIForbindelseMedSamling.tilFakta() =
    FaktaUtgifterIForbindelseMedSamling(
        periodeForSamling =
            this.periodeForSamling.map {
                FaktaPeriodeForSamling(
                    fom = it.fom,
                    tom = it.tom,
                    trengteEkstraOvernatting = it.trengteEkstraOvernatting,
                    utgifterTilOvernatting = it.utgifterTilOvernatting,
                )
            },
    )

private fun UtgifterFlereSteder.tilFakta() =
    FaktaUtgifterFlereSteder(
        delerBoutgifter = this.delerBoutgifter,
        andelUtgifterBoligHjemsted = this.andelUtgifterBoligHjemsted,
        andelUtgifterBoligAktivitetssted = this.andelUtgifterBoligAktivitetssted,
    )

private fun UtgifterNyBolig.tilFakta() =
    FaktaUtgifterNyBolig(
        delerBoutgifter = this.delerBoutgifter,
        andelUtgifterBolig = this.andelUtgifterBolig,
        harHoyereUtgifterPaNyttBosted = this.harHoyereUtgifterPaNyttBosted,
        mottarBostotte = this.mottarBostotte,
    )
