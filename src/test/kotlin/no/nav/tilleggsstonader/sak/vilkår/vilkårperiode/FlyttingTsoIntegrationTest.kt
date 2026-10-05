package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlyttingTsoIntegrationTest : CleanDatabaseIntegrationTest() {
    private val ident = "12345678910"

    @Test
    fun `skal kunne gjennomføre inngangsvilkår for flytting tso`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.FLYTTING_TSO,
                identer = setOf(PersonIdent(ident = ident)),
            )

        opprettOgTilordneOppgaveForBehandling(behandling.id)

        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id, tilSteg = StegType.VILKÅR) {
            defaultFlyttingTSOTestdata()
        }

        val behandlingEtterInngangsvilkår = kall.behandling.hent(behandling.id)
        assertThat(behandlingEtterInngangsvilkår.steg).isEqualTo(StegType.VILKÅR)

        val vilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder
        assertThat(vilkårperioder.målgrupper).hasSize(1)
        assertThat(vilkårperioder.aktiviteter).hasSize(1)
    }
}
