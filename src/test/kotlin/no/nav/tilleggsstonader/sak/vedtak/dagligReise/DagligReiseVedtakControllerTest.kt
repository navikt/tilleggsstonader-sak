package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkEmpty
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkårDagligReise
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatOffentligTransportDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
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
                ),
            gjelderFraOgMed = dummyFom,
            gjelderTilOgMed = dummyTom,
            begrunnelse = null,
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
        val vedtakRequest = InnvilgelseDagligReiseRequest(listOf(vedtaksperiode.tilDto()))

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
}
