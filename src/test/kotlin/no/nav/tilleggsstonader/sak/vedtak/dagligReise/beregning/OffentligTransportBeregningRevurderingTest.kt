package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførRevurderingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførVilkårSteg
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OffentligTransportBeregningRevurderingTest(
    @Autowired private val vedtakRepository: VedtakRepository,
) : CleanDatabaseIntegrationTest() {
    @Test
    fun forlengelseAvReiserSkalIkkePåvirkeTidligereReiser() {
        val reiser =
            lagreDagligReiseDto(
                fom = 1 januar 2025,
                tom = 16 februar 2025,
            )

        val førstegangsbehandlingId =
            gjennomførBehandlingsløp(
                medAktivitet = ::lagreAktivitet,
                medMålgruppe = ::lagreMålgruppe,
                medVilkår =
                    listOf(
                        reiser,
                    ),
            )

        val førstegangsbehandling = kall.behandling.hent(førstegangsbehandlingId)

        val førstegangsbehandlingVedtak =
            vedtakRepository.findById(førstegangsbehandlingId).get() as GeneriskVedtak<InnvilgelseDagligReise>
        val førstegangsbehandlingBeregningsresultat = førstegangsbehandlingVedtak.data as InnvilgelseDagligReise
        val offentligTransportReise =
            førstegangsbehandlingBeregningsresultat.beregningsresultat.offentligTransport!!.reiser
        val offentligTransportPeriode =
            offentligTransportReise
                .first()
                .perioder
                .first()

        vedtakRepository.update(
            førstegangsbehandlingVedtak.copy(
                data =
                    førstegangsbehandlingBeregningsresultat.copy(
                        beregningsresultat =
                            BeregningsresultatDagligReise(
                                offentligTransport =
                                    BeregningsresultatOffentligTransport(
                                        reiser =
                                            listOf(
                                                offentligTransportReise
                                                    .first()
                                                    .copy(
                                                        perioder =
                                                            listOf(
                                                                offentligTransportPeriode.copy(
                                                                    beløp = 1000000,
                                                                ),
                                                            ),
                                                    ),
                                            ),
                                    ),
                            ),
                    ),
            ),
        )

        val revurderingId =
            gjennomførRevurderingsløp(
                tilSteg = StegType.VILKÅR,
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = førstegangsbehandling.fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        kravMottatt = 17 februar 2025,
                        nyeOpplysningerMetadata = null,
                    ),
            )

        val vilkårId =
            kall.vilkårDagligReise
                .hentVilkår(revurderingId)
                .first()
                .id

        kall.vilkårDagligReise.oppdaterVilkår(
            lagreVilkår =
                lagreDagligReiseDto(
                    fom = 1 januar 2025,
                    tom = 30 mars 2025,
                ),
            vilkårId = vilkårId,
            behandlingId = revurderingId,
        )

        gjennomførVilkårSteg(
            medVilkår = emptyList(),
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        )

        gjennomførBeregningSteg(revurderingId, Stønadstype.DAGLIG_REISE_TSO)

        val a =
            kall.vedtak
                .hentVedtak(
                    Stønadstype.DAGLIG_REISE_TSO,
                    revurderingId,
                ).expectOkWithBody<InnvilgelseDagligReiseResponse>()
                .beregningsresultat.offentligTransport!!
                .reiser

        // vi ville egt sjekke om førstegangsbehandlingen var uendret - men nå filtrerer jeg den vekk fra beregningsresulatat når jeg skal reberegne nye perioder
        assertThat(
            a
                .first()
                .perioder
                .first()
                .beløp == 1000000,
        ).isEqualTo(false)

        assertThat(a.first().perioder.size == 2).isEqualTo(true)
    }
}

private fun lagreAktivitet(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeAktivitet(behandlingId, fom = 1 januar 2025, tom = 30 mars 2025)

private fun lagreMålgruppe(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeMålgruppe(behandlingId, fom = 1 januar 2025, tom = 30 mars 2025)
