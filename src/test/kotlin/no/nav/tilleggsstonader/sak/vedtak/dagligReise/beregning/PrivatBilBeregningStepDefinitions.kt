package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.finnRelevantKilometerSats
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused", "ktlint:standard:function-naming")
class PrivatBilBeregningStepDefinitions {
    val behandlingServiceMock = mockk<BehandlingService>()
    val vilkårServiceMock = mockk<VilkårService>()
    val vilkårRepositoryFake = VilkårRepositoryFake()
    val unleashServiceMock = mockk<UnleashService>()

    val dagligReiseVilkårService =
        DagligReiseVilkårService(
            vilkårRepository = vilkårRepositoryFake,
            vilkårService = vilkårServiceMock,
            behandlingService = behandlingServiceMock,
            unleashService = unleashServiceMock,
        )

    val behandlingId = BehandlingId.random()

    val beregningService =
        PrivatBilBeregningService()

    var reiser: List<VilkårDagligReise> = emptyList()

    var vedtaksperioder: List<Vedtaksperiode> = emptyList()

    var beregningsResultat: BeregningsresultatPrivatBil? = null
    var forventetBeregningsresultat: List<BeregningsresultatUkeCucumber> = emptyList()
    var feil: Exception? = null

    @Gitt("følgende vedtaksperioder for daglig reise privat bil")
    fun `følgende vedtaksperioder`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Gitt("følgende vilkår for daglig reise med privat bil")
    fun `følgende vilkår for daglig reise med privat bil`(dataTable: DataTable) {
        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.VILKÅR,
            )

        every { unleashServiceMock.isEnabled(any()) } returns true

        reiser =
            dataTable.mapRad { rad ->
                val nyttVilkår = mapTilVilkårDagligReise(TypeDagligReise.PRIVAT_BIL, rad)
                dagligReiseVilkårService.opprettNyttVilkår(behandlingId = behandlingId, nyttVilkår = nyttVilkår)
            }
    }

    @Når("beregner for daglig reise privat bil")
    fun `beregner for daglig reise privat bil`() {
        try {
            beregningsResultat = beregningService.beregn(vedtaksperioder, reiser)
        } catch (e: Exception) {
            feil = e
        }
    }

    @Så("forventer vi følgende beregningsrsultat for daglig reise privatBil")
    fun `forventer vi følgende beregningsrsultat for daglig reise privat bil`(dataTable: DataTable) {
        assertThat(feil).isNull()

        val forventetBeregningsresultatForReise = mapUker(dataTable)

        forventetBeregningsresultatForReise.forEachIndexed { index, uke ->
            val gjeldendeReise = beregningsResultat!!.reiser[uke.reiseNr - 1]

            assertThat(gjeldendeReise.uker[index].grunnlag.fom).isEqualTo(uke.grunnlag.fom)
            assertThat(gjeldendeReise.uker[index].grunnlag.tom).isEqualTo(uke.grunnlag.tom)
            assertThat(
                gjeldendeReise.uker[index].grunnlag.antallDagerDenneUkaSomKanDekkes,
            ).isEqualTo(uke.grunnlag.antallDagerDenneUkaSomKanDekkes)
            assertThat(gjeldendeReise.uker[index].grunnlag.antallDagerInkludererHelg).isEqualTo(uke.grunnlag.antallDagerInkludererHelg)
            assertThat(gjeldendeReise.uker[index].stønadsbeløp).isEqualTo(uke.stønadsbeløp)
        }
    }

    @Så("forvent følgende feilmelding for beregning privat bil: {}")
    fun `forvent følgelde feilmelding for beregning privat bil`(feilmelding: String) {
        assertThat(feil?.message).contains(feilmelding)
    }

    @Så("forvent at det ikke finnes et beregninsresultat for privat bil")
    fun `forvent at det ikke finnes et beregninsresultat`() {
        assertThat(beregningsResultat).isNull()
    }

    private fun mapUker(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            val fom = parseDato(DomenenøkkelFelles.FOM, rad)
            val tom = parseDato(DomenenøkkelFelles.TOM, rad)
            BeregningsresultatUkeCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                stønadsbeløp = parseInt(DomenenøkkelFelles.BELØP, rad),
                grunnlag =
                    BeregningsgrunnlagForUke(
                        fom = fom,
                        tom = tom,
                        antallDagerDenneUkaSomKanDekkes = parseInt(DomenenøkkelPrivatBil.ANTALL_DAGER_DEKT_UKE, rad),
                        antallDagerInkludererHelg = parseBoolean(DomenenøkkelPrivatBil.INKLUDERER_HELG, rad),
                        vedtaksperioder = emptyList(),
                        kilometersats = finnRelevantKilometerSats(Datoperiode(fom, tom)),
                    ),
            )
        }

    private fun kjørMedFeilkontekst(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // logger.error(e.message, e)
            feil = e
        }
    }
}

enum class DomenenøkkelPrivatBil(
    override val nøkkel: String,
) : Domenenøkkel {
    ANTALL_REISEDAGER_PER_UKE("Antall reisedager per uke"),
    REISEAVSTAND_EN_VEI("Reiseavstand"),
    BOMPENGER("Bompenger"),
    FERGEKOSTNAD("Fergekostnad"),
    REISENR("Reisenr"),
    ANTALL_DAGER_DEKT_UKE("Antall dager dekt i uke"),
    INKLUDERER_HELG("Inkluderer helg"),
}

data class BeregningsresultatUkeCucumber(
    val reiseNr: Int,
    val grunnlag: BeregningsgrunnlagForUke,
    val stønadsbeløp: Int,
)
