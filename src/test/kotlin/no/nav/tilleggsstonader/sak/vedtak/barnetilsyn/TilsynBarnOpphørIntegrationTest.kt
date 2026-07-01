package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilsynBarnOpphørIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal lagre vedtak`() {
        val fom = 1 januar 2025
        val tom = 28 februar 2025

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BARNETILSYN,
            ) {
                defaultTilsynBarnTestdata(fom = fom, tom = tom)
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val behandlingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
            ) {
                aktivitet {
                    oppdaterTomPåEnesteAktivitet(tom = fom.toYearMonth().atEndOfMonth())
                }
                vedtak {
                    opphør(
                        opphørsdato = 31 januar 2025,
                    )
                }
            }

        val vedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.BARNETILSYN, behandlingId)
                .expectOkWithBody<OpphørTilsynBarnResponse>()

        assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(vedtak.årsakerOpphør).containsExactly(ÅrsakOpphør.ANNET)
        assertThat(vedtak.begrunnelse).isEqualTo("annet")
    }

    /**
     * I tilfeller der AAP opphører 2 februar 2025 som er en søndag, så revurderer man fra og med 3 februar 2025.
     * I dette tilfelle kommer det finnes en beløpsperiode for 1 og 2 februar, med beløp 0kr.
     * Problemet her er at dato i [Beløpsperiode] er 3 januar, då den settes til første ukesdagen i perioden.
     * Så hadde vi tidligere validering at det ikke finnes noen beløpsperioder fra og med revurder-fra.
     * Nå validerer vi kun at det ikke finnes noen beløpsperioder med et beløp større enn 0kr
     */
    @Test
    fun `skal kunne opphøre første mandag i måneden der det finnes helgdager før mandagen`() {
        val fom = 1 januar 2025
        val tom = 28 februar 2025

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BARNETILSYN,
            ) {
                defaultTilsynBarnTestdata(fom = fom, tom = tom)
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val behandlingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
            ) {
                vedtak {
                    opphør(
                        opphørsdato = 3 februar 2025,
                    )
                }
            }

        kall.vedtak
            .hentVedtak(
                stønadstype = Stønadstype.BARNETILSYN,
                behandlingId = behandlingId,
            ).expectOkWithBody<OpphørTilsynBarnResponse>()
            .also { vedtak ->
                assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
                assertThat(vedtak.årsakerOpphør).containsExactly(ÅrsakOpphør.ANNET)
                assertThat(vedtak.begrunnelse).isEqualTo("annet")
            }
    }

//    @Test
//    fun `skal ikke kunne opphøre dersom utgifter endres før opphørsdato`() {
//        val fom = 1 januar 2025
//        val tom = 31 mars 2025
//
//        val førstegangsbehandlingContext =
//            opprettBehandlingOgGjennomførBehandlingsløp(
//                stønadstype = Stønadstype.BARNETILSYN,
//            ) {
//                aktivitet {
//                    opprett {
//                        aktivitetTiltakTilsynBarn(
//                            fom = fom,
//                            tom = tom,
//                            aktivitetsdager = 4,
//                        )
//                    }
//                }
//                målgruppe {
//                    opprett {
//                        målgruppeAAP(fom, tom)
//                    }
//                }
//                vilkår {
//                    opprett {
//                        passBarn(
//                            fom = fom.toYearMonth().plusMonths(1),
//                            tom = tom.toYearMonth(),
//                            utgift = 1000,
//                        )
//                    }
//                }
//            }
//
//        val revurderingId =
//            opprettRevurderingOgGjennomførBehandlingsløp(
//                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
//                tilSteg = StegType.BEREGNE_YTELSE,
//            ) {
//                vilkår {
//                    oppdater { vilkårsvurdering ->
//                        with(vilkårsvurdering.vilkårsett.single()) {
//                            SvarPåVilkårDto(
//                                id = id,
//                                behandlingId = behandlingId,
//                                delvilkårsett = delvilkårsett,
//                                fom = fom.plusMonths(1),
//                                tom = 28 februar 2025,
//                                utgift = 500,
//                                erFremtidigUtgift = erFremtidigUtgift,
//                                offentligTransport = null,
//                            )
//                        }
//                    }
//                }
//            }
//
//        kall.vedtak.apiRespons
//            .lagreOpphør(
//                stønadstype = Stønadstype.BARNETILSYN,
//                behandlingId = revurderingId,
//                opphørDto =
//                    opphørDto(
//                        opphørsdato = 1 mars 2025,
//                    ),
//            ).expectProblemDetail(
//                forventetStatus = HttpStatus.BAD_REQUEST,
//                forventetDetail =
//                    "Opphør er et ugyldig vedtaksresultat fordi " +
//                        "opphørsdato er etter eller lik tidligste endring (01.02.2025)",
//            )
//    }
}
