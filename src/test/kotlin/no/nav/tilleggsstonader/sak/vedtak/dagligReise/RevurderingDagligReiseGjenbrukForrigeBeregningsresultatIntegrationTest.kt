package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class RevurderingDagligReiseGjenbrukForrigeBeregningsresultatIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
) : IntegrationTest() {
    val fom: LocalDate = 2 januar 2026
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
                    oppdaterEnesteAktivitet { this.copy(begrunnelse = "oppdatert begrunnelse") }
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

    @Test
    fun `revurdering privat bil med omfant GJENBRUK_FORRIGE skal gjenbruke forrige resultat`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager = listOf(KjørtDag(dato = fom, parkeringsutgift = 50))
                }
            }

        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(førstegangsbehandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        gjennomførKjørelisteBehandling(kjørelistebehandling)

        val vedtakKjørelistebehandling =
            vedtakService.hentVedtak<InnvilgelseDagligReise>(kjørelistebehandling.id).data

        // Kun begrunnelse endres, så tidligsteEndring skal forbli null → GJENBRUK_FORRIGE_RESULTAT.
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                aktivitet {
                    oppdaterEnesteAktivitet { this.copy(begrunnelse = "oppdatert begrunnelse") }
                }
            }

        val vedtakRevurdering = vedtakService.hentVedtak<InnvilgelseDagligReise>(revurderingId).data

        assertThat(vedtakRevurdering.beregningsplan.omfang).isEqualTo(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT)
        assertThat(vedtakRevurdering.beregningsresultat.privatBil)
            .isEqualTo(vedtakKjørelistebehandling.beregningsresultat.privatBil)
    }

    @Test
    fun `revurdering med bytte fra offentlig transport til privat bil skal ikke beholde beregningsresultat for offentlig transport`() {
        every { unleashService.isEnabled(Toggle.KAN_REVURDERE_PRIVAT_BIL) } returns true

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(fom, tom)
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vilkår {
                    slettDagligReise { vilkår ->
                        vilkår.single { it.fakta.type == TypeDagligReise.OFFENTLIG_TRANSPORT }.id to
                            SlettVilkårRequestDto(kommentar = "Skal bytte til privat bil")
                    }
                    opprett {
                        privatBil(fom = fom, tom = tom, reisedagerPerUke = 5)
                    }
                }
            }

        val vedtakRevurdering = vedtakService.hentVedtak<InnvilgelseDagligReise>(revurderingId).data

        assertThat(vedtakRevurdering.beregningsresultat.offentligTransport).isNull()
    }
}
