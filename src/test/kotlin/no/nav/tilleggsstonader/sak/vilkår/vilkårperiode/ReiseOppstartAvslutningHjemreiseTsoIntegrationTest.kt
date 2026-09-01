package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// TODO: Utvid testen etter hvert som søknadsmapping, stønadsvilkår og beregning for
//  STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO blir implementert.
class ReiseOppstartAvslutningHjemreiseTsoIntegrationTest : CleanDatabaseIntegrationTest() {
    val fom = 1 januar 2026
    val tom = 31 januar 2026
    val ident = "12345678910"

    @Test
    fun `skal kunne lagre målgruppe og aktivitet for reise oppstart, avslutning og hjemreise TSO`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                identer = setOf(PersonIdent(ident = ident)),
            )

        opprettOgTilordneOppgaveForBehandling(behandling.id)

        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id, tilSteg = StegType.VILKÅR) {
            defaultReiseTilOppstartAvslutningOgHjemreiserTSOTestdata(fom, tom)
        }

        val vilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder

        assertThat(vilkårperioder.målgrupper).hasSize(1)
        assertThat(vilkårperioder.aktiviteter).hasSize(1)

        val målgruppe = vilkårperioder.målgrupper.single()
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)

        val aktivitet = vilkårperioder.aktiviteter.single()
        assertThat(aktivitet.type).isEqualTo(AktivitetType.TILTAK)
    }
}
