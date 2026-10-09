package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.AvslagFlyttingDto
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FlyttingTsoIntegrationTest : IntegrationTest() {
    private val ident = FnrGenerator.generer()

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `skal gjennomføre avslag for flytting tso`() {
        val fom = 1 januar 2026
        val tom = 1 januar 2026
        val behandling =
            testoppsettService
                .opprettBehandlingMedFagsak(
                    behandling = behandling(),
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    identer = setOf(PersonIdent(ident = ident)),
                )

        opprettOgTilordneOppgaveForBehandling(behandling.id)
        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsoFlytting(fom, tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingEgenKjøring(fom, tom, avstandEnVei = 100, bompenger = 100)
            }
            vedtak {
                avslag()
            }
        }

        val ferdigbehandling = kall.behandling.hent(behandling.id)
        assertThat(ferdigbehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(ferdigbehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        val avslag =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, behandling.id)
                .expectOkWithBody<AvslagFlyttingDto>()

        assertThat(avslag.årsakerAvslag).isEqualTo(listOf(ÅrsakAvslag.ANNET))
        assertThat(avslag.type).isEqualTo(TypeVedtak.AVSLAG)
        assertThat(tilkjentYtelseRepository.findByBehandlingId(behandling.id)).isNull()
    }

    @Test
    fun `skal kunne vedta og beregne flytting med flytting selv`() {
        val fom = 1 januar 2026
        val tom = 1 januar 2026

        val behandling =
            testoppsettService
                .opprettBehandlingMedFagsak(
                    behandling = behandling(),
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    identer = setOf(PersonIdent(ident = ident)),
                )

        opprettOgTilordneOppgaveForBehandling(behandling.id)
        gjennomførBehandlingsløp(
            ident = ident,
            behandlingId = behandling.id,
        ) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsoFlytting(fom, tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingEgenKjøring(
                    fom,
                    tom,
                    avstandEnVei = 100,
                    bompenger = 100,
                )
            }
        }

        val vedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, behandling.id)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()

        assertThat(vedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
        assertThat(vedtak.beregningsresultat.resultater).hasSize(1)

        val resultat = vedtak.beregningsresultat.resultater.single()

        assertThat(resultat.beløp).isEqualTo(394.toBigDecimal())
        assertThat(resultat.grunnlag).isInstanceOf(BeregningsgrunnlagEgenKjøring::class.java)

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse

        assertThat(andeler).hasSize(1)
        assertThat(andeler.single().beløp).isEqualTo(394)
        assertThat(andeler.single().type).isEqualTo(TypeAndel.FLYTTING_AAP)
    }

    @Test
    fun `skal kunne vedta og beregne flytting med flyttebyrå, og utbetale andeler hvis dokumentert`() {
        val fom = 1 januar 2026
        val tom = 1 januar 2026

        val behandling =
            testoppsettService
                .opprettBehandlingMedFagsak(
                    behandling = behandling(),
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    identer = setOf(PersonIdent(ident = ident)),
                )

        opprettOgTilordneOppgaveForBehandling(behandling.id)
        gjennomførBehandlingsløp(
            ident = ident,
            behandlingId = behandling.id,
        ) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsoFlytting(fom, tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingByrå(
                    fom,
                    tom,
                    tilbud1Pris = 10000,
                    tilbud2Pris = 5000,
                    erBetalingDokumentert = true,
                )
            }
        }

        val vedtakFørstegangsbehandling =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, behandling.id)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()

        assertThat(vedtakFørstegangsbehandling.beregningsresultat.resultater).hasSize(1)

        val resultatFørstegangsbehandling = vedtakFørstegangsbehandling.beregningsresultat.resultater.single()

        assertThat(resultatFørstegangsbehandling.beløp).isEqualTo(5000.toBigDecimal())
        assertThat(resultatFørstegangsbehandling.grunnlag).isInstanceOf(BeregningsgrunnlagFlyttebyrå::class.java)

        val andelerFørstegangsbehandling =
            tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse

        assertThat(andelerFørstegangsbehandling).hasSize(1)
        assertThat(andelerFørstegangsbehandling.single().beløp).isEqualTo(5000)
        assertThat(andelerFørstegangsbehandling.single().type).isEqualTo(TypeAndel.FLYTTING_AAP)

        testoppsettService.settAndelerTilOkForBehandling(behandling.id)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = behandling.id) {
                vilkår {
                    endreFlytting {
                        val byrå = fakta as FaktaFlyttebyråDto
                        copy(fakta = byrå.copy(tilbud2 = byrå.tilbud2.copy(pris = 3000)))
                    }
                }
            }

        val vedtakRevurdering =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, revurderingId)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()

        assertThat(vedtakRevurdering.beregningsresultat.resultater).hasSize(1)

        val resultatRevurdering = vedtakRevurdering.beregningsresultat.resultater.single()

        assertThat(resultatRevurdering.beløp).isEqualTo(3000.toBigDecimal())
        assertThat(resultatRevurdering.grunnlag).isInstanceOf(BeregningsgrunnlagFlyttebyrå::class.java)

        val andelerRevurdering =
            tilkjentYtelseRepository.findByBehandlingId(revurderingId)!!.andelerTilkjentYtelse

        assertThat(andelerRevurdering).hasSize(1)

        val nyAndel = andelerRevurdering.find { it.beløp == 3000 }

        assertThat(nyAndel).isNotNull()
    }

    @Test
    fun `skal kunne vedta og beregne flytting med flyttebyrå, og ikke utbetale andeler hvis ikke dokumentert`() {
        val fom = 1 januar 2026
        val tom = 1 januar 2026

        val behandling =
            testoppsettService
                .opprettBehandlingMedFagsak(
                    behandling = behandling(),
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    identer = setOf(PersonIdent(ident = ident)),
                )

        opprettOgTilordneOppgaveForBehandling(behandling.id)
        gjennomførBehandlingsløp(
            ident = ident,
            behandlingId = behandling.id,
        ) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsoFlytting(fom, tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingByrå(
                    fom,
                    tom,
                    tilbud1Pris = 10000,
                    tilbud2Pris = 5000,
                    erBetalingDokumentert = false,
                )
            }
        }

        val vedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, behandling.id)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()

        assertThat(vedtak.beregningsresultat.resultater).hasSize(1)

        val resultat = vedtak.beregningsresultat.resultater.single()

        assertThat(resultat.beløp).isEqualTo(5000.toBigDecimal())
        assertThat(resultat.grunnlag).isInstanceOf(BeregningsgrunnlagFlyttebyrå::class.java)

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse

        assertThat(andeler).isEmpty()
    }
}
