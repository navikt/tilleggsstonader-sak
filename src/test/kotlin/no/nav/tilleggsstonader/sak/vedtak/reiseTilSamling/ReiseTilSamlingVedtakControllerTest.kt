package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkEmpty
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.BeregningsresultatReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingTsoRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
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
    val aktivitet =
        aktivitet(dummyBehandlingId, fom = fom, tom = tom)

    val målgruppe = målgruppe(dummyBehandlingId, fom = fom, tom = tom)

    val dummyVedtaksperiode = vedtaksperiode(fom = fom, tom = tom)

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(dummyBehandling, stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO)
        opprettOgTilordneOppgaveForBehandling(dummyBehandling.id)

        vilkårperiodeRepository.insert(aktivitet)
        vilkårperiodeRepository.insert(målgruppe)
        vilkårRepository.insert(
            vilkår(
                behandlingId = dummyBehandlingId,
                type = VilkårType.REISE_TIL_SAMLING,
                resultat = Vilkårsresultat.OPPFYLT,
                status = VilkårStatus.NY,
                fom = fom,
                tom = tom,
                fakta =
                    FaktaReiseTilSamlingOffentligTransport(
                        reiseId = dummyReiseId,
                        adresse = "Samlingsgata 1",
                        utgifterOffentligTransport = 500.toBigDecimal(),
                    ),
            ),
        )
    }

    @Test
    fun `hent vedtak skal returnere tom body når det ikke finnes noen lagrede vedtak`() {
        kall.vedtak
            .hentVedtak(Stønadstype.REISE_TIL_SAMLING_TSO, dummyBehandlingId)
            .expectOkEmpty()
    }

    @Nested
    inner class Beregn {
        @Test
        fun `beregner offentlig transport basert på relevante vilkårperioder`() {
            val utgifter = 500.toBigDecimal()

            val request = InnvilgelseReiseTilSamlingTsoRequest(listOf(vedtaksperiode(fom = fom, tom = tom).tilDto()))

            val respons =
                kall.testklient
                    .post("/api/vedtak/reise-til-samling/$dummyBehandlingId/tso/beregn", request)
                    .expectOkWithBody<BeregningsresultatReiseTilSamlingDto>()

            val offentligTransport = checkNotNull(respons.offentligTransport)
            assertThat(offentligTransport.reiser).hasSize(1)

            val reise = offentligTransport.reiser.single()
            assertThat(reise.reiseId).isEqualTo(dummyReiseId)
            assertThat(reise.adresse).isEqualTo("Samlingsgata 1")
            assertThat(reise.fom).isEqualTo(fom)
            assertThat(reise.tom).isEqualTo(tom)
            assertThat(reise.beløp).isEqualTo(utgifter)
        }
    }
}
