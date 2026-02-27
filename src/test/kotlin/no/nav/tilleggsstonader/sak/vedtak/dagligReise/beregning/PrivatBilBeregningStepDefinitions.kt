package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
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
import no.nav.tilleggsstonader.sak.cucumber.parseBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.SatsDagligReisePrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.satser
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

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

    val satsDagligReisePrivatBilProvider = SatsDagligReisePrivatBilProvider()
    val beregningService =
        PrivatBilBeregningService(satsDagligReisePrivatBilProvider)

    var reiser: List<VilkårDagligReise> = emptyList()

    var vedtaksperioder: List<Vedtaksperiode> = emptyList()

    var rammevedtak: RammevedtakPrivatBil? = null
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
            rammevedtak = beregningService.beregnRammevedtak(vedtaksperioder, reiser)
        } catch (e: Exception) {
            feil = e
        }
    }

    @Så("forventer vi rammevedtak for følgende periode")
    fun `forventer vi følgende beregningsrsultat for følgende periode`(dataTable: DataTable) {
        assertThat(feil).isNull()

        val forventetRammevedtakForReise = mapRammevedtak(dataTable)

        forventetRammevedtakForReise.forEach { forventetRammevedtak ->
            val gjeldendeReise = rammevedtak!!.reiser[forventetRammevedtak.reiseNr - 1]

            assertThat(gjeldendeReise.grunnlag.fom).isEqualTo(forventetRammevedtak.fom)
            assertThat(gjeldendeReise.grunnlag.tom).isEqualTo(forventetRammevedtak.tom)
            assertThat(gjeldendeReise.grunnlag.reisedagerPerUke).isEqualTo(forventetRammevedtak.reisedagerPerUke)
        }
    }

    @Og("vi forventer følgende satser for rammevedtak")
    fun `vi forventer følgende satser for rammevedtak`(dataTable: DataTable) {
        val forventedeSatserPerReise = mapForventedeSatser(dataTable).groupBy { it.reiseNr }

        forventedeSatserPerReise.forEach { (reiseNr, satserForReise) ->
            val satserIRammevedtak = rammevedtak!!.reiser[reiseNr - 1].grunnlag.satser
            assertThat(satserIRammevedtak).hasSameSizeAs(satserForReise)
            satserForReise.forEachIndexed { index, forventetSats ->
                val satsIRammevedtak = satserIRammevedtak[index]
                assertThat(satsIRammevedtak.fom).isEqualTo(forventetSats.fom)
                assertThat(satsIRammevedtak.tom).isEqualTo(forventetSats.tom)
                assertThat(satsIRammevedtak.dagsatsUtenParkering).isEqualTo(forventetSats.dagsatsUtenParkering)
                assertThat(satsIRammevedtak.kilometersats).isEqualTo(forventetSats.kilometersats)
                assertThat(satsIRammevedtak.satsBekreftetVedVedtakstidspunkt).isEqualTo(forventetSats.satsBekreftetVedVedtakstidspunkt)
            }
        }
    }

    @Og("vi forventer følgende vedtaksperioder for rammevedtak med reiseNr={}")
    fun `vi forventer følgende vedtaksperioder for rammevedtak`(
        reiseNr: Int,
        dataTable: DataTable,
    ) {
        val forventedeVedtaksperioder = mapVedtaksperioder(dataTable)

        // Sammenlign uten uuid da den genereres i mapVedtaksperioder() over
        val dummyUuid = UUID.randomUUID()
        assertThat(
            rammevedtak!!
                .reiser[reiseNr - 1]
                .grunnlag.vedtaksperioder
                .map { it.copy(id = dummyUuid) },
        ).isEqualTo(
            forventedeVedtaksperioder.map { it.copy(id = dummyUuid) },
        )
    }

    private fun Periode<LocalDate>.tilDatoPeriode() = Datoperiode(fom, tom)

    @Så("forvent følgende feilmelding for beregning privat bil: {}")
    fun `forvent følgelde feilmelding for beregning privat bil`(feilmelding: String) {
        assertThat(feil?.message).contains(feilmelding)
    }

    @Så("forvent at det ikke finnes et beregninsresultat for privat bil")
    fun `forvent at det ikke finnes et beregninsresultat`() {
        assertThat(rammevedtak).isNull()
    }

    private fun mapRammevedtak(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            RammevedtakCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                reisedagerPerUke = parseInt(DomenenøkkelPrivatBil.ANTALL_REISEDAGER_PER_UKE, rad),
            )
        }

    fun mapForventedeInnvilgedePerioder(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            InnvilgetPeriodeIRammevedtakCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
            )
        }

    fun mapForventedeSatser(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            ForventetSatsCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                dagsatsUtenParkering = parseBigDecimal(DomenenøkkelPrivatBil.DAGSATS_UTEN_PARKERING, rad),
                kilometersats = parseBigDecimal(DomenenøkkelPrivatBil.KILOMETERSATS, rad),
                satsBekreftetVedVedtakstidspunkt = parseBoolean(DomenenøkkelPrivatBil.SATS_BEKREFTET, rad),
            )
        }

    private fun mapSatser(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            SatsForPeriodePrivatBilCucumber(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                satsBekreftetVedVedtakstidspunkt = true,
                kilometersats = parseBigDecimal(DomenenøkkelPrivatBil.KILOMETERSATS, rad),
                dagsatsUtenParkering = parseBigDecimal(DomenenøkkelPrivatBil.DAGSATS_UTEN_PARKERING, rad),
            )
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
    DAGSATS_UTEN_PARKERING("Dagsats uten parkering"),
    KILOMETERSATS("Kilometersats"),
    SATS_BEKREFTET("Sats bekreftet"),
}

data class RammevedtakCucumber(
    val reiseNr: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
) : Periode<LocalDate>

data class SatsForPeriodePrivatBilCucumber(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal,
) : Periode<LocalDate>

data class InnvilgetPeriodeIRammevedtakCucumber(
    val reiseNr: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

data class ForventetSatsCucumber(
    val reiseNr: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsatsUtenParkering: BigDecimal,
    val kilometersats: BigDecimal,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
)

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
