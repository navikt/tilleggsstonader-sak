package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LæremidlerOpphørIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal lagre vedtak`() {
        val fom = 1 januar 2025
        val tom = 30 april 2025

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.LÆREMIDLER,
            ) {
                defaultLæremidlerTestdata(fom = fom, tom = tom)
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
            ) {
                vedtak {
                    opphør(
                        opphørsdato = 1 februar 2025,
                    )
                }
            }

        val vedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.LÆREMIDLER, revurderingId)
                .expectOkWithBody<OpphørLæremidlerResponse>()

        assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(vedtak.årsakerOpphør).containsExactly(ÅrsakOpphør.ANNET)
        assertThat(vedtak.begrunnelse).isEqualTo("annet")
        with(vedtak.vedtaksperioder.single()) {
            assertThat(this.fom).isEqualTo(fom)
            assertThat(this.tom).isEqualTo(31 januar 2025)
        }
    }

//    @Test
//    fun `skal ikke kunne opphøre med endringer i vilkårperiode før opphørsdato`() {
//        val fom = 1 januar 2025
//        val tom = 31 mars 2025
//
//        val førstegangsbehandlingContext =
//            opprettBehandlingOgGjennomførBehandlingsløp(
//                stønadstype = Stønadstype.LÆREMIDLER,
//            ) {
//                aktivitet {
//                    opprett {
//                        aktivitetUtdanningLæremidler(fom, tom)
//                    }
//                }
//                målgruppe {
//                    opprett {
//                        målgruppeAAP(fom, tom)
//                    }
//                }
//            }
//
//        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)
//
//        val behandlingId =
//            opprettRevurderingOgGjennomførBehandlingsløp(
//                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
//                tilSteg = StegType.BEREGNE_YTELSE,
//            ) {
//                målgruppe {
//                    oppdaterTomPåEnesteMålgruppe(28 februar 2025)
//                }
//            }
//
//        kall.vedtak.apiRespons
//            .lagreOpphør(
//                stønadstype = Stønadstype.LÆREMIDLER,
//                behandlingId = behandlingId,
//                opphørDto = opphørDto(opphørsdato = 1 mars 2025),
//            ).expectProblemDetail(
//                forventetStatus = HttpStatus.BAD_REQUEST,
//                forventetDetail =
//                    "Opphør er et ugyldig vedtaksresultat fordi " +
//                        "opphørsdato er etter eller lik tidligste endring (01.03.2025)",
//            )
//    }
}
