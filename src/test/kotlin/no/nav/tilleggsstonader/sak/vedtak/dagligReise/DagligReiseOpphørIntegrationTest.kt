package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DagligReiseOpphørIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val tilkjentYtelseService: TilkjentYtelseService,
) : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal kunne opphøre`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(2 januar 2025, 6 juni 2025)
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vedtak {
                    opphør(opphørsdato = 15 mars 2025)
                }
            }

        val beregningsresultat = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId).data.beregningsresultat
        assertThat(
            beregningsresultat.offentligTransport!!
                .reiser[0]
                .perioder
                .maxByOrNull {
                    it.grunnlag.tom
                }!!
                .grunnlag.tom,
        ).isEqualTo(14 mars 2025)

        val andelerFørstegangsbehandling =
            tilkjentYtelseService
                .hentForBehandling(
                    førstegangsbehandlingContext.behandlingId,
                ).andelerTilkjentYtelse
        val andelerOpphør = tilkjentYtelseService.hentForBehandling(revurderingId).andelerTilkjentYtelse

        assertThat(andelerOpphør.size).isNotEqualTo(andelerFørstegangsbehandling.size)
        assertThat(andelerOpphør.maxByOrNull { it.tom }!!.tom).isBeforeOrEqualTo(14 mars 2025)
    }

//    @Test
//    fun `skal ikke kunne opphøre dersom pris er endret før opphørsdato`() {
//        val fom = 1 januar 2025
//        val tom = 31 mars 2025
//
//        val førstegangsbehandlingContext =
//            opprettBehandlingOgGjennomførBehandlingsløp(
//                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
//            ) {
//                aktivitet {
//                    opprett {
//                        aktivitetTiltakTso(fom, tom)
//                    }
//                }
//                målgruppe {
//                    opprett {
//                        målgruppeAAP(fom, tom)
//                    }
//                }
//                vilkår {
//                    opprett {
//                        offentligTransport(
//                            fom = fom.plusMonths(1),
//                            tom = tom,
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
//                    oppdaterDagligReise { vilkårDagligReise, _ ->
//                        vilkårDagligReise.single().id to
//                            lagreDagligReiseDto(
//                                fom = fom.plusMonths(1),
//                                tom = 28 februar 2025,
//                                fakta =
//                                    FaktaDagligReiseOffentligTransportDto(
//                                        reisedagerPerUke = 3,
//                                        prisEnkelbillett = 40,
//                                        prisSyvdagersbillett = null,
//                                        prisTrettidagersbillett = 1200,
//                                    ),
//                            )
//                    }
//                }
//            }
//
//        kall.vedtak.apiRespons
//            .lagreOpphør(
//                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
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
