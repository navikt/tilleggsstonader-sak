package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkEmpty
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.junit.jupiter.api.BeforeEach
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
}
