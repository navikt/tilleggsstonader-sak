package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
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
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.SatsDagligReisePrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDelperiodePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
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
    val vilkårperiodeRepositoryMock = mockk<VilkårperiodeRepository>()
    val vilkårperiodeService = mockk<VilkårperiodeService>()

    val dagligReiseVilkårService =
        DagligReiseVilkårService(
            vilkårRepository = vilkårRepositoryFake,
            vilkårService = vilkårServiceMock,
            behandlingService = behandlingServiceMock,
            unleashService = unleashServiceMock,
            vilkårperiodeService = vilkårperiodeService,
        )

    val behandlingId = BehandlingId.random()

    val satsDagligReisePrivatBilProvider = SatsDagligReisePrivatBilProvider()
    val beregningService =
        PrivatBilBeregningService(
            satsDagligReisePrivatBilProvider = satsDagligReisePrivatBilProvider,
            vilkårperiodeService = vilkårperiodeService,
        )

    var reiserUtenDelperioder: Map<Int, LagreDagligReise> = emptyMap()
    var delperioderForReisenummer: Map<Int, List<FaktaDelperiodePrivatBil>> = emptyMap()

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

        reiserUtenDelperioder =
            dataTable
                .mapRad { rad ->
                    val fom = parseDato(DomenenøkkelFelles.FOM, rad)
                    val tom = parseDato(DomenenøkkelFelles.TOM, rad)

                    val testAktivitet =
                        aktivitet(
                            behandlingId = behandlingId,
                            fom = fom,
                            tom = tom,
                            faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                            resultat = ResultatVilkårperiode.OPPFYLT,
                            typeAktivitet = TypeAktivitet.GRUPPEAMO,
                        )
                    every { vilkårperiodeService.hentAktivitet(testAktivitet.globalId, behandlingId) } returns testAktivitet

                    parseInt(DomenenøkkelPrivatBil.REISENR, rad) to
                        mapTilVilkårDagligReise(
                            typeVilkår = TypeDagligReise.PRIVAT_BIL,
                            rad = rad,
                            aktivitetId = testAktivitet.globalId,
                        )
                }.toMap()
    }

    @Gitt("følgende delperioder for vilkår daglig reise med privat bil")
    fun `følgende delperioder for vilkår daglig reise med privat bil`(dataTable: DataTable) {
        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.VILKÅR,
            )

        every { unleashServiceMock.isEnabled(any()) } returns true

        delperioderForReisenummer = mapDelperioder(dataTable)
    }

    @Når("beregner for daglig reise privat bil")
    fun `beregner for daglig reise privat bil`() {
        val oppfylteReisevilkår =
            reiserUtenDelperioder.map { (reiseNr, lagreDagligReise) ->
                dagligReiseVilkårService.opprettNyttVilkår(
                    behandlingId = behandlingId,
                    nyttVilkår =
                        lagreDagligReise.copy(
                            fakta =
                                (lagreDagligReise.fakta as FaktaPrivatBil).copy(
                                    faktaDelperioder =
                                        delperioderForReisenummer[reiseNr]
                                            ?: error("Må angi delperioder for reiseNr $reiseNr"),
                                ),
                        ),
                )
            }

        try {
            rammevedtak =
                beregningService.beregnRammevedtak(
                    vedtaksperioder = vedtaksperioder,
                    oppfylteVilkår = oppfylteReisevilkår,
                    behandlingId = behandlingId,
                )
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
            assertThat(gjeldendeReise.grunnlag.reiseavstandEnVei).isEqualTo(forventetRammevedtak.reiseavstandEnVei)
        }
    }

    @Og("forventer vi rammevedtak for følgende delperioder")
    fun `forventer vi rammevedtak for følgende delperioder`(dataTable: DataTable) {
        assertThat(feil).isNull()

        val forventetRammevedtakForReise = mapRammevedtak(dataTable)

        forventetRammevedtakForReise.forEach { forventetRammevedtak ->
            val gjeldendeReise = rammevedtak!!.reiser[forventetRammevedtak.reiseNr - 1]

            assertThat(gjeldendeReise.grunnlag.fom).isEqualTo(forventetRammevedtak.fom)
            assertThat(gjeldendeReise.grunnlag.tom).isEqualTo(forventetRammevedtak.tom)
            assertThat(gjeldendeReise.grunnlag.reiseavstandEnVei).isEqualTo(forventetRammevedtak.reiseavstandEnVei)
        }
    }

    @Og("vi forventer følgende delperioder for rammevedtak")
    fun `vi forventer følgende delperioder for rammevedtak`(dataTable: DataTable) {
        val forventedeDelperioderPerReise = mapDelperiodeCucumber(dataTable).groupBy { it.reiseNr }
        forventedeDelperioderPerReise.forEach { (reiseNr, delperioderForReise) ->
            val delPerioderIRammevedtak = rammevedtak!!.reiser[reiseNr - 1].grunnlag.delperioder
            assertThat(delPerioderIRammevedtak).hasSameSizeAs(delperioderForReise)
            delperioderForReise.forEachIndexed { index, forventetSats ->
                val delperiode = delPerioderIRammevedtak[index]
                assertThat(delperiode.fom).isEqualTo(forventetSats.fom)
                assertThat(delperiode.tom).isEqualTo(forventetSats.tom)
                assertThat(delperiode.reisedagerPerUke).isEqualTo(forventetSats.reisedagerPerUke)
            }
        }
    }

    @Og("vi forventer følgende satser for delperioder")
    fun `vi forventer følgende satser for delperioder`(dataTable: DataTable) {
        val forventedeSatsDelperioderPerReise = mapSatsDelperiodeCucumber(dataTable).groupBy { it.reiseNr }
        forventedeSatsDelperioderPerReise.forEach { (reiseNr, forventedeDelperiodeSatserForReise) ->
            val delperioderIRammevedtak = rammevedtak!!.reiser[reiseNr - 1].grunnlag.delperioder

            val satserForDelperiode: Map<Int, List<SatsDelperiodeCucumber>> =
                forventedeDelperiodeSatserForReise.groupBy { it.delperiodeNr }
            satserForDelperiode.forEach { (delperiodeNr, forventedeSatserForDelperiode) ->
                val delperiodeIRammevedtak = delperioderIRammevedtak[delperiodeNr - 1]
                assertThat(delperiodeIRammevedtak.satser).hasSameSizeAs(forventedeSatserForDelperiode)

                forventedeSatserForDelperiode.forEachIndexed { index, forventetSatsIDelperiode ->
                    val satsForDelperiodeIRammevedtak = delperiodeIRammevedtak.satser[index]
                    assertThat(satsForDelperiodeIRammevedtak.fom).isEqualTo(forventetSatsIDelperiode.fom)
                    assertThat(satsForDelperiodeIRammevedtak.tom).isEqualTo(forventetSatsIDelperiode.tom)
                    assertThat(satsForDelperiodeIRammevedtak.dagsatsUtenParkering).isEqualTo(forventetSatsIDelperiode.dagsatsUtenParkering)
                    assertThat(
                        satsForDelperiodeIRammevedtak.satsBekreftetVedVedtakstidspunkt,
                    ).isEqualTo(forventetSatsIDelperiode.satsBekreftetVedVedtakstidspunkt)
                    assertThat(satsForDelperiodeIRammevedtak.kilometersats).isEqualTo(forventetSatsIDelperiode.kilometersats)
                }
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

    private fun mapDelperioder(dataTable: DataTable): Map<Int, List<FaktaDelperiodePrivatBil>> =
        dataTable
            .mapRad {
                parseInt(DomenenøkkelPrivatBil.REISENR, it) to
                    FaktaDelperiodePrivatBil(
                        fom = parseDato(DomenenøkkelFelles.FOM, it),
                        tom = parseDato(DomenenøkkelFelles.TOM, it),
                        bompengerPerDag = parseValgfriInt(DomenenøkkelPrivatBil.BOMPENGER, it),
                        fergekostnadPerDag = parseValgfriInt(DomenenøkkelPrivatBil.FERGEKOSTNAD, it),
                        reisedagerPerUke = parseInt(DomenenøkkelPrivatBil.ANTALL_REISEDAGER_PER_UKE, it),
                    )
            }.groupBy { it.first }
            .mapValues { it.value.map { it.second } }

    private fun mapRammevedtak(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            val fom = parseDato(DomenenøkkelFelles.FOM, rad)
            val tom = parseDato(DomenenøkkelFelles.TOM, rad)
            val reiseavstandEnVei = parseBigDecimal(DomenenøkkelPrivatBil.REISEAVSTAND_EN_VEI, rad)
            RammevedtakCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                fom = fom,
                tom = tom,
                reiseavstandEnVei = reiseavstandEnVei,
            )
        }

    fun mapDelperiodeCucumber(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            DelperiodeCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                reisedagerPerUke = parseInt(DomenenøkkelPrivatBil.ANTALL_REISEDAGER_PER_UKE, rad),
            )
        }

    fun mapSatsDelperiodeCucumber(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            SatsDelperiodeCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                delperiodeNr = parseInt(DomenenøkkelPrivatBil.DELPERIODENR, rad),
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
    DELPERIODENR("DelperiodeNr"),
}

data class RammevedtakCucumber(
    val reiseNr: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reiseavstandEnVei: BigDecimal? = null,
) : Periode<LocalDate>

data class DelperiodeCucumber(
    val reiseNr: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
) : Periode<LocalDate>

data class SatsDelperiodeCucumber(
    val reiseNr: Int,
    val delperiodeNr: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val dagsatsUtenParkering: BigDecimal,
    val kilometersats: BigDecimal,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
) : Periode<LocalDate>

data class SatsForPeriodePrivatBilCucumber(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal,
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
