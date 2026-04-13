package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class RevurderingDagligReiseGjenbrukForrigeBeregningsresultatIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
) : IntegrationTest() {
    val fom: LocalDate = 1 januar 2026
    val tom: LocalDate = 31 januar 2026

    /**
     * Når kun ikke-beregningsrelevante felt endres (f.eks. begrunnelse, kildeId), skal
     * tidligsteEndring være null og beregningsplanen være GJENBRUK_FORRIGE_RESULTAT.
     * Beregningsresultatet skal kopieres direkte fra forrige behandling uten ny beregning.
     *
     * Typisk eksempel: en aktivitet bytter kildeId i tiltaksregisteret (samme aktivitet,
     * ny ekstern id), og saksbehandler oppdaterer vilkårperioden – ingen endring som påvirker
     * beregningen, men fremdeles en revurdering.
     */
    @Test
    fun `revurdering der kun ikke-beregningsrelevante felt endres skal gjenbruke beregningsresultatet fra forrige behandling`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(fom, tom)
            }

        val vedtakFørstegangsbehandling =
            vedtakService.hentVedtak<InnvilgelseDagligReise>(førstegangsbehandlingContext.behandlingId).data

        // Kun begrunnelse endres, så tidligsteEndring skal forbli null → GJENBRUK_FORRIGE_RESULTAT.
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                aktivitet {
                    oppdater { aktiviteter, behandlingId ->
                        with(aktiviteter.single()) {
                            id to tilLagreVilkårperiodeAktivitet(behandlingId).copy(begrunnelse = "oppdatert begrunnelse")
                        }
                    }
                }
                vedtak {
                    innvilgelse(
                        vedtaksperioder =
                            listOf(
                                VedtaksperiodeDto(
                                    fom = fom,
                                    tom = tom,
                                    målgruppeType = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                    aktivitetType = AktivitetType.TILTAK,
                                ),
                            ),
                    )
                }
            }

        val vedtakRevurdering = vedtakService.hentVedtak<InnvilgelseDagligReise>(revurderingId).data

        assertThat(vedtakRevurdering.beregningsplan.omfang).isEqualTo(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT)
        assertThat(vedtakRevurdering.beregningsresultat.offentligTransport)
            .isEqualTo(vedtakFørstegangsbehandling.beregningsresultat.offentligTransport)
    }
}
