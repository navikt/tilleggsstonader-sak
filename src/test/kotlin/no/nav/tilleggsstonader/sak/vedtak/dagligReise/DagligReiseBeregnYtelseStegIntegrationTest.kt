package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.OpprettOpphør
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DagligReiseBeregnYtelseStegIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val iverksettService: IverksettService,
) : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal kunne opphøre`() {
        val førstekangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                medAktivitet = langtvarendeAktivitet,
                medMålgruppe = langtvarendeMålgruppe,
                medVilkår = listOf(lagreDagligReiseDto(fom = 2 januar 2025, tom = 6 juni 2025)),
            )

        val førstegangsbehandling = kall.behandling.hent(førstekangsbehandlingId)
        val fagsakId = førstegangsbehandling.fagsakId

        val revurderingId =
            opprettRevurdering(
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        nyeOpplysningerMetadata = null,
                        kravMottatt = 15 mars 2025,
                    ),
            )

        gjennomførBehandlingsløp(
            revurderingId,
            opprettVedtak =
                OpprettOpphør(
                    årsaker = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
                    begrunnelse = "avluttet aktivitet",
                    opphørsdato = 15 mars 2025,
                ),
            tilSteg = StegType.SIMULERING,
            testdataProvider = {},
        )

        val beregningsresultat = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId)!!.data.beregningsresultat
        assertThat(
            beregningsresultat.offentligTransport!!
                .reiser[0]
                .perioder
                .maxByOrNull {
                    it.grunnlag.tom
                }!!
                .grunnlag.tom,
        ).isEqualTo(14 mars 2025)

        val andelerFørstegangsbehandling = iverksettService.hentAndelTilkjentYtelse(førstekangsbehandlingId)
        val andelerOpphør = iverksettService.hentAndelTilkjentYtelse(revurderingId)

        assertThat(andelerOpphør.size).isNotEqualTo(andelerFørstegangsbehandling.size)
        assertThat(andelerOpphør.maxByOrNull { it.tom }!!.tom).isBeforeOrEqualTo(14 mars 2025)
    }

    private val langtvarendeMålgruppe = fun(behandlingId: BehandlingId) =
        lagreVilkårperiodeMålgruppe(
            behandlingId,
            fom = 2 januar 2025,
            tom = 6 juni 2025,
        )
    private val langtvarendeAktivitet = fun (behandlingId: BehandlingId) =
        lagreVilkårperiodeAktivitet(
            behandlingId,
            fom = 2 januar 2025,
            tom = 6 juni 2025,
        )
}
