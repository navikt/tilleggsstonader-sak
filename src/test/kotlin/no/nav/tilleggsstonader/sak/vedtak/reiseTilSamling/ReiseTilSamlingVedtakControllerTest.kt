package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
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
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.BeregningsresultatForSamlingDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.BeregningsresultatOffentligTransportDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.BeregningsresultatReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingResponse
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingTsoRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ReiseTilSamlingVedtakControllerTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    val dummyFagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO)
    val dummyBehandlingId = BehandlingId.random()
    val dummyBehandling =
        behandling(
            id = dummyBehandlingId,
            fagsak = dummyFagsak,
            steg = StegType.BEREGNE_YTELSE,
            status = BehandlingStatus.UTREDES,
        )

    val fom = 1 januar 2025
    val tom = 31 januar 2025

    val dummyVedtaksperiode = vedtaksperiode(fom = fom, tom = tom)

    val dummyInnvilgelse =
        InnvilgelseReiseTilSamlingResponse(
            vedtaksperioder =
                listOf(
                    LagretVedtaksperiodeDto(
                        id = dummyVedtaksperiode.id,
                        fom = fom,
                        tom = tom,
                        målgruppeType = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitetType = AktivitetType.TILTAK,
                        vedtaksperiodeFraForrigeVedtak = null,
                    ),
                ),
            beregningsresultat =
                BeregningsresultatReiseTilSamlingDto(
                    offentligTransport =
                        BeregningsresultatOffentligTransportDto(
                            samling =
                                listOf(
                                    BeregningsresultatForSamlingDto(
                                        reiseId = dummyReiseId,
                                        adresse = "Samlingsgata 1",
                                        fom = fom,
                                        tom = tom,
                                        utgifterOffentligTransport = 500,
                                    ),
                                ),
                            beløp = 500,
                        ),
                ),
            gjelderFraOgMed = fom,
            gjelderTilOgMed = tom,
            begrunnelse = null,
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(dummyBehandling, stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO)
        opprettOgTilordneOppgaveForBehandling(dummyBehandling.id)
    }

    @Test
    fun `hent vedtak skal returnere tom body når det ikke finnes noen lagrede vedtak`() {
        kall.vedtak
            .hentVedtak(Stønadstype.REISE_TIL_SAMLING_TSO, dummyBehandlingId)
            .expectOkEmpty()
    }

//    @Test
//    fun `hent ut lagrede vedtak av type innvilgelse`() {
//        val vedtakRequest = InnvilgelseReiseTilSamlingTsoRequest(listOf(dummyVedtaksperiode.tilDto()))
//        kall.vedtak.lagreInnvilgelse(
//            Stønadstype.REISE_TIL_SAMLING_TSO,
//            dummyBehandlingId,
//            vedtakRequest,
//        )
//        val respons =
//            kall.vedtak
//                .hentVedtak(Stønadstype.REISE_TIL_SAMLING_TSO, dummyBehandlingId)
//                .expectOkWithBody<InnvilgelseReiseTilSamlingResponse>()
//        assertThat(respons).isEqualTo(dummyInnvilgelse)
//    }

    @Nested
    inner class Beregn {
        @Test
        fun `beregn med offentlig transport vilkår returnerer beregningsresultat`() {
            val utgifter = 500
            vilkårRepository.insert(
                vilkår(
                    behandlingId = dummyBehandlingId,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = fom,
                    tom = tom,
                    utgift = null,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Samlingsgata 1",
                            utgifterOffentligTransport = utgifter,
                        ),
                ),
            )

            val request = InnvilgelseReiseTilSamlingTsoRequest(listOf(vedtaksperiode(fom = fom, tom = tom).tilDto()))

            val respons =
                kall.testklient
                    .post("/api/vedtak/reise-til-samling/$dummyBehandlingId/tso/beregn", request)
                    .expectOkWithBody<BeregningsresultatReiseTilSamlingDto>()

            val offentligTransport = checkNotNull(respons.offentligTransport)
            assertThat(offentligTransport.beløp).isEqualTo(utgifter)
            assertThat(offentligTransport.samling).hasSize(1)

            val samling = offentligTransport.samling.single()
            assertThat(samling.reiseId).isEqualTo(dummyReiseId)
            assertThat(samling.adresse).isEqualTo("Samlingsgata 1")
            assertThat(samling.fom).isEqualTo(fom)
            assertThat(samling.tom).isEqualTo(tom)
            assertThat(samling.utgifterOffentligTransport).isEqualTo(utgifter)
        }
    }
}
