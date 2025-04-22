package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.BoligEllerOvernattingAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.DelerUtgifterFlereStederType
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.FasteUtgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.TypeFasteUtgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.TypeUtgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.UtgifterFlereSteder
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.UtgifterIForbindelseMedSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.UtgifterNyBolig

data class FaktaBoligEllerOvernatting(
    val søknadsgrunnlag: FaktaBoligEllerOvernattingSøknadsgrunnlag?,
)

data class FaktaBoligEllerOvernattingSøknadsgrunnlag(
    val typeUtgifter: TypeUtgifter,
    val fasteUtgifter: FaktaFasteUtgifter?,
    val samling: UtgifterIForbindelseMedSamling?,
    val harSærligStoreUtgifterPgaFunksjonsnedsettelse: JaNei,
)

data class FaktaFasteUtgifter(
    val typeFasteUtgifter: TypeFasteUtgifter,
    val utgifterFlereSteder: FaktaUtgifterFlereSteder?,
    val utgifterNyBolig: UtgifterNyBolig?,
)

data class FaktaUtgifterFlereSteder(
    val delerBoutgifter: List<DelerUtgifterFlereStederType>,
    val andelUtgifterBoligHjemsted: Int,
    val andelUtgifterBoligAktivitetssted: Int,
)

fun BoligEllerOvernattingAvsnitt.tilFakta() =
    FaktaBoligEllerOvernattingSøknadsgrunnlag(
        typeUtgifter = this.typeUtgifter,
        fasteUtgifter = this.fasteUtgifter?.tilFakta(),
        samling = this.samling,
        harSærligStoreUtgifterPgaFunksjonsnedsettelse = this.harSærligStoreUtgifterPgaFunksjonsnedsettelse,
    )

private fun FasteUtgifter.tilFakta() =
    FaktaFasteUtgifter(
        typeFasteUtgifter = this.typeFasteUtgifter,
        utgifterFlereSteder = this.utgifterFlereSteder?.tilFakta(),
        utgifterNyBolig = this.utgifterNyBolig,
    )

private fun UtgifterFlereSteder.tilFakta() =
    FaktaUtgifterFlereSteder(
        delerBoutgifter = this.delerBoutgifter,
        andelUtgifterBoligHjemsted = this.andelUtgifterBoligHjemsted,
        andelUtgifterBoligAktivitetssted = this.andelUtgifterBoligAktivitetssted,
    )
