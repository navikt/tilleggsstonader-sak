package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.finnPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.StønadUtbetaling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FlyttingRevurderingIntegrationTest : IntegrationTest() {
    private val ident = FnrGenerator.generer()

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `skal kunne revurdere flytting og se at andeler stemmer for nye vilkårsvurderinger`() {
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
                    tilbud1Pris = 10000,
                    tilbud2Pris = 5000,
                    erBetalingDokumentert = true,
                )
            }
        }

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
    fun `skal kunne etterdokumentere flyttevilkår og kun utbetale andel for dokumentert byrå`() {
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
                    tilbud1Pris = 10000,
                    tilbud2Pris = 5000,
                    erBetalingDokumentert = false,
                )
            }
        }

        testoppsettService.settAndelerTilOkForBehandling(behandling.id)

        val andelerFørstegangsbehandling =
            tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse

        assertThat(andelerFørstegangsbehandling).hasSize(0)
        assertThat(KafkaFake.sendteMeldinger().finnPåTopic(kafkaTopics.utbetaling)).isEmpty()

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = behandling.id) {
                vilkår {
                    endreFlytting {
                        val byrå = fakta as FaktaFlyttebyråDto
                        copy(fakta = byrå.copy(erBetalingDokumentert = true))
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(revurderingId)

        val vedtakRevurdering =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, revurderingId)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()

        assertThat(vedtakRevurdering.beregningsresultat.resultater).hasSize(1)

        val resultatRevurdering = vedtakRevurdering.beregningsresultat.resultater.single()

        assertThat(resultatRevurdering.beløp).isEqualTo(5000.toBigDecimal())
        assertThat(resultatRevurdering.grunnlag).isInstanceOf(BeregningsgrunnlagFlyttebyrå::class.java)

        val andelerRevurdering =
            tilkjentYtelseRepository.findByBehandlingId(revurderingId)!!.andelerTilkjentYtelse

        assertThat(andelerRevurdering).hasSize(1)
        val nyAndel = andelerRevurdering.find { it.beløp == 5000 }
        assertThat(nyAndel).isNotNull()

        val sisteIverksetting = sisteUtbetalingIverksetting()
        assertThat(sisteIverksetting.utbetalinger).hasSize(1)
        assertThat(sisteIverksetting.utbetalinger.single().perioder).hasSize(1)
        assertThat(
            sisteIverksetting.utbetalinger
                .single()
                .perioder
                .single()
                .beløp,
        ).isEqualTo(nyAndel!!.beløp.toUInt())
    }

    @Test
    fun `skal kunne trekke tilbake at flytting er dokumentert i revurdering slik at ingenting utbetales`() {
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
                    tilbud1Pris = 10000,
                    tilbud2Pris = 5000,
                    erBetalingDokumentert = true,
                )
            }
        }

        testoppsettService.settAndelerTilOkForBehandling(behandling.id)

        val andelerFørstegangsbehandling =
            tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse

        assertThat(andelerFørstegangsbehandling).hasSize(1)
        val førsteIverksetting = sisteUtbetalingIverksetting()
        assertThat(førsteIverksetting.utbetalinger).hasSize(1)
        assertThat(førsteIverksetting.utbetalinger.single().perioder).hasSize(1)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = behandling.id) {
                vilkår {
                    endreFlytting {
                        val byrå = fakta as FaktaFlyttebyråDto
                        copy(fakta = byrå.copy(erBetalingDokumentert = false))
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(revurderingId)

        val vedtakRevurdering =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, revurderingId)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()

        assertThat(vedtakRevurdering.beregningsresultat.resultater).hasSize(1)

        val resultatRevurdering = vedtakRevurdering.beregningsresultat.resultater.single()

        assertThat(resultatRevurdering.beløp).isEqualTo(5000.toBigDecimal())
        assertThat(resultatRevurdering.fraTidligereVedtak).isFalse
        assertThat((resultatRevurdering.grunnlag as BeregningsgrunnlagFlyttebyrå).erBetalingDokumentert).isFalse
        assertThat(resultatRevurdering.grunnlag).isInstanceOf(BeregningsgrunnlagFlyttebyrå::class.java)

        val andelerRevurdering =
            tilkjentYtelseRepository.findByBehandlingId(revurderingId)!!.andelerTilkjentYtelse

        // En ugyldig andel for å kunne sende opphør til hel-ved og motta iverksetting-status
        assertThat(andelerRevurdering.single().type).isEqualTo(TypeAndel.UGYLDIG)
        val sisteIverksetting = sisteUtbetalingIverksetting()

        // Verifiserer i praksis opphøret
        assertThat(sisteIverksetting.utbetalinger.single { it.stønad == StønadUtbetaling.FLYTTING_AAP }.perioder).isEmpty()
    }

    @Test
    fun `skal kunne endre dato på flytting frem i tid i en revurdering`() {
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
                    aktivitetTiltakTsoFlytting(fom, tom.plusDays(7))
                }
            }

            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom.plusDays(7))
                }
            }
            vilkår {
                flyttingByrå(
                    fom,
                    tilbud1Pris = 10000,
                    tilbud2Pris = 5000,
                    erBetalingDokumentert = true,
                )
            }
        }

        testoppsettService.settAndelerTilOkForBehandling(behandling.id)

        val andelerFørstegangsbehandling =
            tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse

        assertThat(andelerFørstegangsbehandling).hasSize(1)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = behandling.id) {
                vilkår {
                    endreFlytting {
                        copy(flytteDato = fom.plusDays(7))
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(revurderingId)

        val vedtakRevurdering =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, revurderingId)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()

        assertThat(vedtakRevurdering.beregningsresultat.resultater).hasSize(1)

        val resultatRevurdering = vedtakRevurdering.beregningsresultat.resultater.single()

        assertThat(resultatRevurdering.beløp).isEqualTo(5000.toBigDecimal())
        assertThat(resultatRevurdering.grunnlag).isInstanceOf(BeregningsgrunnlagFlyttebyrå::class.java)

        val andelerRevurdering =
            tilkjentYtelseRepository.findByBehandlingId(revurderingId)!!.andelerTilkjentYtelse

        assertThat(andelerRevurdering).hasSize(1)
        val nyAndel = andelerRevurdering.find { it.beløp == 5000 }
        assertThat(nyAndel).isNotNull()
    }

    private fun sisteUtbetalingIverksetting() =
        KafkaFake
            .sendteMeldinger()
            .finnPåTopic(kafkaTopics.utbetaling)
            .map { it.verdiEllerFeil<IverksettingDto>() }
            .maxBy { it.vedtakstidspunkt }
}
