package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsoDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// TODO: Utvid testen etter hvert som søknadsmapping, stønadsvilkår og beregning for
//  STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO blir implementert.
class ReiseOppstartAvslutningHjemreiseTsoIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal kunne lagre målgruppe og aktivitet for reise oppstart, avslutning og hjemreise TSO`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
            )
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        kall.vilkårperiode.opprett(
            lagreVilkårperiodeMålgruppe(
                behandlingId = behandling.id,
                målgruppeType = MålgruppeType.AAP,
            ),
        )
        kall.vilkårperiode.opprett(
            lagreVilkårperiodeAktivitet(
                behandlingId = behandling.id,
                aktivitetType = AktivitetType.TILTAK,
                faktaOgSvar =
                    FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsoDto(
                        svarLønnet = SvarJaNei.NEI,
                        svarHarUtgifter = SvarJaNei.JA,
                        svarErAktivitetenObligatorisk = SvarJaNei.JA,
                    ),
            ),
        )

        val vilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder

        assertThat(vilkårperioder.målgrupper).hasSize(1)
        assertThat(vilkårperioder.aktiviteter).hasSize(1)

        val målgruppe = vilkårperioder.målgrupper.single()
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)

        val aktivitet = vilkårperioder.aktiviteter.single()
        assertThat(aktivitet.type).isEqualTo(AktivitetType.TILTAK)
    }
}
