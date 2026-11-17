package no.nav.tilleggsstonader.sak.vedtak.forslag

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForeslåVedtaksperiodeServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val vedtakService = mockk<VedtakService>()
    private val utledTidligsteEndringService = mockk<UtledTidligsteEndringService>()

    private val service =
        ForeslåVedtaksperiodeService(
            behandlingService = behandlingService,
            vilkårperiodeService = vilkårperiodeService,
            vilkårService = vilkårService,
            vedtakService = vedtakService,
            utledTidligsteEndringService = utledTidligsteEndringService,
        )

    @Test
    fun `skal foreslå vedtaksperioder fra reisene og ikke fra paraplyvilkåret`() {
        val saksbehandling =
            saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO))
        val behandlingId = saksbehandling.id

        val paraply =
            vilkår(
                behandlingId = behandlingId,
                type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
                fom = 1 januar 2026,
                tom = 31 mars 2026,
            )
        val oppstart =
            vilkår(
                behandlingId = behandlingId,
                type = VilkårType.ROAH_OPPSTART_AVSLUTNING,
                fom = 1 januar 2026,
                tom = 5 januar 2026,
            ).copy(parentVilkår = paraply.globalId)
        val hjemreise =
            vilkår(
                behandlingId = behandlingId,
                type = VilkårType.ROAH_HJEMREISE,
                fom = 1 mars 2026,
                tom = 3 mars 2026,
            ).copy(parentVilkår = paraply.globalId)

        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
            Vilkårperioder(
                målgrupper = listOf(målgruppe(fom = 1 januar 2026, tom = 31 mars 2026)),
                aktiviteter = listOf(aktivitet(fom = 1 januar 2026, tom = 31 mars 2026)),
            )
        every { vilkårService.hentVilkår(behandlingId) } returns listOf(paraply, oppstart, hjemreise)
        every {
            utledTidligsteEndringService.utledTidligsteEndringIgnorerVedtaksperioder(behandlingId)
        } returns null

        val forslag = service.foreslåPerioder(behandlingId)

        assertThat(forslag.map { it.fom to it.tom }).containsExactly(
            (1 januar 2026) to (5 januar 2026),
            (1 mars 2026) to (3 mars 2026),
        )
    }
}
