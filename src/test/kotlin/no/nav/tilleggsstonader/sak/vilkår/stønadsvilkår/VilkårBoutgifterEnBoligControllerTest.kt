package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.BoutgifterEnBoligDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterEnBolig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VilkårBoutgifterEnBoligControllerTest : CleanDatabaseIntegrationTest() {
    private val fagsak = fagsak(stønadstype = Stønadstype.BOUTGIFTER)
    private val behandling = behandling(fagsak, status = BehandlingStatus.UTREDES, steg = StegType.VILKÅR)
    private val delvilkår = oppfylteDelvilkårLøpendeUtgifterEnBolig().map { it.tilDto() }

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling, stønadstype = Stønadstype.BOUTGIFTER)
        opprettOgTilordneOppgaveForBehandling(behandling.id)
    }

    // Kan fjernes etter toggle er fjernet
    @Test
    fun `lagrer og leser fakta også etter at toggle slås av`() {
        val opprettet =
            kall.vilkår.opprettVilkår(
                OpprettVilkårDto(
                    vilkårType = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                    behandlingId = behandling.id,
                    delvilkårsett = delvilkår,
                    fom = 1 januar 2026,
                    tom = 31 januar 2026,
                    utgift = null,
                    erFremtidigUtgift = false,
                    offentligTransport = null,
                    boutgifterEnBolig = BoutgifterEnBoligDto(5000, 6000),
                ),
            )
        assertThat(opprettet.utgift).isEqualTo(1000)
        every { unleashService.isEnabled(Toggle.BOUTGIFTER_EN_BOLIG_MERUTGIFTER_FAKTA) } returns false

        val oppdatert =
            kall.vilkår.oppdaterVilkår(
                SvarPåVilkårDto(
                    id = opprettet.id,
                    behandlingId = behandling.id,
                    delvilkårsett = delvilkår,
                    fom = opprettet.fom,
                    tom = opprettet.tom,
                    utgift = null,
                    erFremtidigUtgift = false,
                    offentligTransport = null,
                    boutgifterEnBolig = BoutgifterEnBoligDto(5000, 7000),
                ),
            )

        assertThat(oppdatert.utgift).isEqualTo(2000)
        val hentet =
            kall.vilkår
                .hentVilkår(behandling.id)
                .vilkårsett
                .single()
        assertThat(hentet.boutgifterEnBolig).isEqualTo(BoutgifterEnBoligDto(5000, 7000))
        assertThat(hentet.utgift).isEqualTo(2000)
    }
}
