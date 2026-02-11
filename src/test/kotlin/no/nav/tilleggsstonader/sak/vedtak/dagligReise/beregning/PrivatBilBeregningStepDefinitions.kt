package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.finnRelevantKilometerSats
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.LocalDate

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

    var beregningsResultat: RammevedtakPrivatBil? = null
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
            beregningsResultat = beregningService.beregnRammevedtak(vedtaksperioder, reiser)
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
                gjeldendeReise.uker[index].grunnlag.maksAntallDagerSomKanDekkes,
            ).isEqualTo(uke.grunnlag.maksAntallDagerSomKanDekkes)
            assertThat(gjeldendeReise.uker[index].grunnlag.antallDagerInkludererHelg).isEqualTo(uke.grunnlag.antallDagerInkludererHelg)

            uke.maksBeløpSomKanDekkesFørParkering?.let {
                assertThat(gjeldendeReise.uker[index].maksBeløpSomKanDekkesFørParkering).isEqualTo(
                    it,
                )
            }
            uke.grunnlag.dagsatsUtenParkering?.let {
                assertThat(gjeldendeReise.uker[index].dagsatsUtenParkering).isEqualTo(it)
            }
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
                maksBeløpSomKanDekkesFørParkering = parseValgfriInt(DomenenøkkelFelles.BELØP, rad),
                grunnlag =
                    BeregningsgrunnlagForUkeCucumber(
                        fom = fom,
                        tom = tom,
                        maksAntallDagerSomKanDekkes = parseInt(DomenenøkkelPrivatBil.ANTALL_DAGER_DEKT_UKE, rad),
                        antallDagerInkludererHelg = parseBoolean(DomenenøkkelPrivatBil.INKLUDERER_HELG, rad),
                        vedtaksperioder = emptyList(),
                        kilometersats = finnRelevantKilometerSats(Datoperiode(fom, tom)),
                        dagsatsUtenParkering = parseValgfriBigDecimal(DomenenøkkelPrivatBil.DAGSATS_UTEN_PARKERING, rad),
                    ),
            )
        }
}

enum class DomenenøkkelPrivatBil(
    override val nøkkel: String,
) : Domenenøkkel {
    ANTALL_REISEDAGER_PER_UKE("Reisedager per uke"),
    REISEAVSTAND_EN_VEI("Reiseavstand"),
    BOMPENGER("Bompenger"),
    FERGEKOSTNAD("Fergekostnad"),
    REISENR("Reisenr"),
    ANTALL_DAGER_DEKT_UKE("Antall dager dekt i uke"),
    INKLUDERER_HELG("Inkluderer helg"),
    DAGSATS_UTEN_PARKERING("Dagsats uten parkering"),
}

data class BeregningsresultatUkeCucumber(
    val reiseNr: Int,
    val grunnlag: BeregningsgrunnlagForUkeCucumber,
    val maksBeløpSomKanDekkesFørParkering: Int?,
)

data class BeregningsgrunnlagForUkeCucumber(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val maksAntallDagerSomKanDekkes: Int,
    val antallDagerInkludererHelg: Boolean,
    val vedtaksperioder: List<Vedtaksperiode>,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal?,
) : Periode<LocalDate>
