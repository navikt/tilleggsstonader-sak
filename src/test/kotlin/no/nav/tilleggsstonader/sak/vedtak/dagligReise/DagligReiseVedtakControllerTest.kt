package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.avslåVedtakDagligReise
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.hentVedtakDagligReise
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.hentVedtakDagligReiseKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.innvilgeVedtakDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.BillettType
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.OffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
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

class DagligReiseVedtakControllerTest : IntegrationTest() {
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
        OffentligTransport(
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
                BeregningsresultatDagligReise(
                    offentligTransport =
                        Beregningsresultat(
                            reiser =
                                listOf(
                                    BeregningsresultatForReise(
                                        perioder =
                                            listOf(
                                                BeregningsresultatForPeriode(
                                                    grunnlag =
                                                        Beregningsgrunnlag(
                                                            fom = dummyFom,
                                                            tom = dummyTom,
                                                            prisEnkeltbillett = 44,
                                                            prisSyvdagersbillett = null,
                                                            pris30dagersbillett = 750,
                                                            antallReisedagerPerUke = 4,
                                                            vedtaksperioder =
                                                                listOf(
                                                                    VedtaksperiodeGrunnlag(
                                                                        id = vedtaksperiode.id,
                                                                        fom = dummyFom,
                                                                        tom = dummyTom,
                                                                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                                                        aktivitet = AktivitetType.TILTAK,
                                                                        antallReisedagerIVedtaksperioden = 19,
                                                                    ),
                                                                ),
                                                            antallReisedager = 19,
                                                        ),
                                                    beløp = 750,
                                                    billetDetalijer = mapOf(BillettType.MÅNEDSKORT to 1),
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
        vilkår(
            behandlingId = dummyBehandlingId,
            type = VilkårType.DAGLIG_REISE_OFFENTLIG_TRANSPORT,
            fom = dummyFom,
            tom = dummyTom,
            offentligTransport = dummyOffentligTransport,
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(dummyBehandling, stønadstype = Stønadstype.DAGLIG_REISE_TSO)
        vilkårperiodeRepository.insert(aktivitet)
        vilkårperiodeRepository.insert(målgruppe)
        vilkårRepository.insert(vilkår)
    }

    @Test
    fun `hent vedtak skal returnere tom body når det ikke finnes noen lagrede vedtak`() {
        hentVedtakDagligReiseKall(dummyBehandlingId)
            .expectStatus()
            .isOk
            .expectBody()
            .isEmpty
    }

    @Test
    fun `hent ut lagrede vedtak av type innvilgelse`() {
        val vedtakRequest = InnvilgelseDagligReiseRequest(listOf(vedtaksperiode.tilDto()))

        innvilgeVedtakDagligReise(
            dummyBehandling.id,
            vedtakRequest,
        )
        val response = hentVedtakDagligReise<InnvilgelseDagligReiseResponse>(dummyBehandlingId)

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

            avslåVedtakDagligReise(dummyBehandling.id, avslag)

            val lagretDto = hentVedtakDagligReise<AvslagDagligReiseDto>(dummyBehandling.id)

            assertThat(lagretDto.årsakerAvslag).isEqualTo(avslag.årsakerAvslag)
            assertThat(lagretDto.begrunnelse).isEqualTo(avslag.begrunnelse)
            assertThat(lagretDto.type).isEqualTo(TypeVedtak.AVSLAG)
        }
    }
}
