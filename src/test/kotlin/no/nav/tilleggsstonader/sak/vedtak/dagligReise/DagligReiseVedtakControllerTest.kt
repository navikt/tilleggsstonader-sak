package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkEmpty
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkårDagligReise
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatOffentligTransportDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDelperiodePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class DagligReiseVedtakControllerTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    val dummyFom: LocalDate = LocalDate.parse("2025-01-01")
    val dummyTom: LocalDate = LocalDate.parse("2025-01-30")
    val dummyFagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO)
    val dummyBehandlingId = BehandlingId.random()
    val dummyBehandling =
        behandling(
            id = dummyBehandlingId,
            fagsak = dummyFagsak,
            steg = StegType.BEREGNE_YTELSE,
            status = BehandlingStatus.UTREDES,
        )
    val dummyOffentligTransport =
        FaktaOffentligTransport(
            reiseId = dummyReiseId,
            adresse = "Tiltaksveien 1",
            reisedagerPerUke = 4,
            prisEnkelbillett = 44,
            prisSyvdagersbillett = null,
            prisTrettidagersbillett = 750,
        )

    val vedtaksperiode = vedtaksperiode(fom = dummyFom, tom = dummyTom)
    val aktivitet = aktivitet(dummyBehandlingId, fom = dummyFom, tom = dummyTom)
    val målgruppe = målgruppe(dummyBehandlingId, fom = dummyFom, tom = dummyTom)

    val dummyInnvilgelse =
        InnvilgelseDagligReiseResponse(
            vedtaksperioder =
                listOf(
                    LagretVedtaksperiodeDto(
                        id = vedtaksperiode.id,
                        fom = dummyFom,
                        tom = dummyTom,
                        målgruppeType = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitetType = AktivitetType.TILTAK,
                        vedtaksperiodeFraForrigeVedtak = null,
                    ),
                ),
            beregningsresultat =
                BeregningsresultatDagligReiseDto(
                    offentligTransport =
                        BeregningsresultatOffentligTransportDto(
                            reiser =
                                listOf(
                                    BeregningsresultatForReiseDto(
                                        reiseId = dummyReiseId,
                                        adresse = "Tiltaksveien 1",
                                        perioder =
                                            listOf(
                                                BeregningsresultatForPeriodeDto(
                                                    fom = dummyFom,
                                                    tom = dummyTom,
                                                    prisEnkeltbillett = 44,
                                                    prisSyvdagersbillett = null,
                                                    pris30dagersbillett = 750,
                                                    antallReisedagerPerUke = 4,
                                                    beløp = 750,
                                                    billettdetaljer = mapOf(Billettype.TRETTIDAGERSBILLETT to 1),
                                                    antallReisedager = 19,
                                                    fraTidligereVedtak = false,
                                                    brukersNavKontor = null,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    privatBil = null,
                    beregningsplan =
                        Beregningsplan(
                            omfang = Beregningsomfang.ALLE_PERIODER,
                            fraDato = null,
                        ),
                ),
            gjelderFraOgMed = dummyFom,
            gjelderTilOgMed = dummyTom,
            begrunnelse = null,
            rammevedtakPrivatBil = null,
        )

    val vilkår =
        vilkårDagligReise(
            behandlingId = dummyBehandlingId,
            fom = dummyFom,
            tom = dummyTom,
            fakta = dummyOffentligTransport,
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(dummyBehandling, stønadstype = Stønadstype.DAGLIG_REISE_TSO)
        opprettOgTilordneOppgaveForBehandling(dummyBehandling.id)
        vilkårperiodeRepository.insert(aktivitet)
        vilkårperiodeRepository.insert(målgruppe)
        vilkårRepository.insert(vilkår.mapTilVilkår())
    }

    @Test
    fun `hent vedtak skal returnere tom body når det ikke finnes noen lagrede vedtak`() {
        kall.vedtak
            .hentVedtak(Stønadstype.DAGLIG_REISE_TSO, dummyBehandlingId)
            .expectOkEmpty()
    }

    @Test
    fun `hent ut lagrede vedtak av type innvilgelse`() {
        val vedtakRequest = InnvilgelseDagligReiseTsoRequest(listOf(vedtaksperiode.tilDto()))

        kall.vedtak.lagreInnvilgelse(
            Stønadstype.DAGLIG_REISE_TSO,
            dummyBehandling.id,
            vedtakRequest,
        )
        val response =
            kall.vedtak
                .hentVedtak(Stønadstype.DAGLIG_REISE_TSO, dummyBehandlingId)
                .expectOkWithBody<InnvilgelseDagligReiseResponse>()

        assertThat(response).isEqualTo(dummyInnvilgelse)
    }

    @Nested
    inner class Avslag {
        @Test
        fun `skal lagre og hente avslag`() {
            val avslag =
                AvslagDagligReiseDto(
                    årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                    begrunnelse = "begrunnelse",
                )

            kall.vedtak.lagreAvslag(Stønadstype.DAGLIG_REISE_TSO, dummyBehandling.id, avslag)

            val lagretDto =
                kall.vedtak
                    .hentVedtak(Stønadstype.DAGLIG_REISE_TSO, dummyBehandling.id)
                    .expectOkWithBody<AvslagDagligReiseDto>()

            assertThat(lagretDto.årsakerAvslag).isEqualTo(avslag.årsakerAvslag)
            assertThat(lagretDto.begrunnelse).isEqualTo(avslag.begrunnelse)
            assertThat(lagretDto.type).isEqualTo(TypeVedtak.AVSLAG)
        }
    }

    @Nested
    inner class BeregningOppsummering {
        val fom = 29 desember 2025
        val tom = 18 januar 2026

        val delperioder =
            listOf(
                FaktaDelperiodePrivatBilDto(
                    fom = fom,
                    tom = 4 januar 2026,
                    reisedagerPerUke = 2,
                    bompengerPerDag = 50,
                    fergekostnadPerDag = null,
                ),
                FaktaDelperiodePrivatBilDto(
                    fom = 5 januar 2026,
                    tom = 11 januar 2026,
                    reisedagerPerUke = 3,
                    bompengerPerDag = 50,
                    fergekostnadPerDag = null,
                ),
                FaktaDelperiodePrivatBilDto(
                    fom = 12 januar 2026,
                    tom = 18 januar 2026,
                    reisedagerPerUke = 3,
                    bompengerPerDag = null,
                    fergekostnadPerDag = 100,
                ),
            )

        val innsendteKjørteDager =
            listOf(
                29 desember 2025 to 50,
                1 januar 2026 to 100,
                5 januar 2026 to null,
                6 januar 2026 to null,
                12 januar 2026 to null,
                13 januar 2026 to 60,
                14 januar 2026 to null,
            )

        @Test
        fun `skal oppsummere rammevedtak og beregningsresultat for privat bil i kjørelistebehandling`() {
            every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

            val førstegangsBehandlingContext =
                opprettBehandlingOgGjennomførBehandlingsløp(
                    stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                ) {
                    defaultDagligReisePrivatBilTsoTestdata(
                        fom,
                        tom,
                        reiseavstandEnVei = 10.toBigDecimal(),
                        delperioder = delperioder,
                    )

                    sendInnKjøreliste {
                        periode = Datoperiode(fom, tom)
                        kjørteDager = innsendteKjørteDager
                    }
                }

            val førstegangsBehandling = testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId)

            val kjørelisteBehandling =
                testoppsettService
                    .hentBehandlinger(førstegangsBehandling.fagsakId)
                    .single { it.type == BehandlingType.KJØRELISTE }

            gjennomførKjørelisteBehandling(kjørelisteBehandling, tilSteg = StegType.BEREGNING)

            val oppsummertBeregning = kall.privatBil.hentOppsummertBeregning(kjørelisteBehandling.id)

            assertThat(oppsummertBeregning.reiser).hasSize(1)

            val oppsummertReise = oppsummertBeregning.reiser.single()
            assertThat(oppsummertReise.reiseavstandEnVei).isEqualTo(10.toBigDecimal())
            assertThat(oppsummertReise.perioder).hasSize(3)

            validerOppsummertUke(
                oppsummertUke = oppsummertReise.perioder[0],
                forventetUke =
                    ForventetOppsummertUke(
                        fom = fom,
                        tom = 4 januar 2026,
                        antallGodkjenteReisedager = 2,
                        bompengerTotalt = 100,
                        fergekostnadTotalt = null,
                        satserSize = 2,
                        totalParkeringskostnad = 150,
                        ukenummer = 1,
                        stønadsbeløp = BigDecimal("366.40"),
                    ),
            )

            validerOppsummertUke(
                oppsummertUke = oppsummertReise.perioder[1],
                forventetUke =
                    ForventetOppsummertUke(
                        fom = 5 januar 2026,
                        tom = 11 januar 2026,
                        antallGodkjenteReisedager = 2,
                        bompengerTotalt = 100,
                        fergekostnadTotalt = null,
                        satserSize = 1,
                        totalParkeringskostnad = 0,
                        ukenummer = 2,
                        stønadsbeløp = BigDecimal("217.60"),
                    ),
            )

            validerOppsummertUke(
                oppsummertUke = oppsummertReise.perioder[2],
                forventetUke =
                    ForventetOppsummertUke(
                        fom = 12 januar 2026,
                        tom = tom,
                        antallGodkjenteReisedager = 3,
                        bompengerTotalt = null,
                        fergekostnadTotalt = 300,
                        satserSize = 1,
                        totalParkeringskostnad = 60,
                        ukenummer = 3,
                        stønadsbeløp = BigDecimal("536.40"),
                    ),
            )
        }

        private fun validerOppsummertUke(
            oppsummertUke: OppsummertBeregningForPeriodeDto,
            forventetUke: ForventetOppsummertUke,
        ) {
            assertThat(oppsummertUke.fom).isEqualTo(forventetUke.fom)
            assertThat(oppsummertUke.tom).isEqualTo(forventetUke.tom)
            assertThat(oppsummertUke.antallGodkjenteReisedager).isEqualTo(forventetUke.antallGodkjenteReisedager)
            assertThat(oppsummertUke.bompengerTotalt).isEqualTo(forventetUke.bompengerTotalt)
            assertThat(oppsummertUke.fergekostnadTotalt).isEqualTo(forventetUke.fergekostnadTotalt)
            assertThat(oppsummertUke.satser).hasSize(forventetUke.satserSize)
            assertThat(oppsummertUke.parkeringskostnadTotalt).isEqualTo(forventetUke.totalParkeringskostnad)
            assertThat(oppsummertUke.ukenummer).isEqualTo(forventetUke.ukenummer)
            assertThat(oppsummertUke.stønadsbeløp).isEqualTo(forventetUke.stønadsbeløp)
        }
    }

    data class ForventetOppsummertUke(
        val fom: LocalDate,
        val tom: LocalDate,
        val antallGodkjenteReisedager: Int,
        val bompengerTotalt: Int?,
        val fergekostnadTotalt: Int?,
        val satserSize: Int,
        val totalParkeringskostnad: Int,
        val ukenummer: Int,
        val stønadsbeløp: BigDecimal,
    )
}
