package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeAktivitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BevarGlobalIdMellomBehandlingerIntegrationTest : IntegrationTest() {
    @Test
    fun `opprett to behandlinger, vilkårsperioder skal ha samme globalId for førstegangsbehandling og revurdering`() {
        val fom = 1 januar 2026
        val tom = 31 mai 2026
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.LÆREMIDLER,
            ) {
                defaultLæremidlerTestdata(fom, tom)
            }

        val revurderingBehandlingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.INNGANGSVILKÅR,
            ) {
                // Oppdaterer også aktivitet for å være sikker på at id'en også bevares da
                aktivitet {
                    oppdater { dtos, id ->
                        with(dtos.single()) {
                            this.id to this.tilLagreVilkårperiodeAktivitet(id)
                        }
                    }
                }
            }

        val vilkårperioderFørstegangsbehandling =
            kall.vilkårperiode
                .hentForBehandling(
                    førstegangsbehandlingContext.behandlingId,
                ).vilkårperioder
        val vilkårperioderRevurdering = kall.vilkårperiode.hentForBehandling(revurderingBehandlingId).vilkårperioder

        assertThat(vilkårperioderFørstegangsbehandling.aktiviteter.single().globalId)
            .isEqualTo(vilkårperioderRevurdering.aktiviteter.single().globalId)
        assertThat(vilkårperioderFørstegangsbehandling.målgrupper.single().globalId)
            .isEqualTo(vilkårperioderRevurdering.målgrupper.single().globalId)
    }
}
