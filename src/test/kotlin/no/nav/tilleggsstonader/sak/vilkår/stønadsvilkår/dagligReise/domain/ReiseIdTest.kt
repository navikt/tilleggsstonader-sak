package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReiseIdTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `reiseId blir overført fra vilkår til beregningsresultat`() {
        val behandlingId = gjennomførBehandlingsløp()

        val vilkår =
            kall.vilkårDagligReise
                .hentVilkår(behandlingId)
                .single()
                .fakta as FaktaDagligReiseOffentligTransportDto

        val reiseIdBeregningsresultat =
            kall.vedtak
                .hentVedtak(Stønadstype.DAGLIG_REISE_TSO, behandlingId)
                .expectOkWithBody<InnvilgelseDagligReiseResponse>()
                .beregningsresultat.offentligTransport!!
                .reiser
                .single()
                .reiseId

        assertThat(reiseIdBeregningsresultat).isEqualTo(vilkår.reiseId)
    }
}
