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
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingResponse
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.OpphørFlyttingResponse
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
    fun `skal kunne innvilge flytting og senere opphøre den`() {
        val fom = 1 januar 2026
        val tom = fom
        val opphørsdato = fom

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
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingByrå(
                    flytteDato = fom,
                    tilbudPriser = listOf(10000, 5000),
                    erBetalingDokumentert = true,
                )
            }
        }

        testoppsettService.settAndelerTilOkForBehandling(behandling.id)

        val opphørId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = behandling.id) {
                vedtak {
                    opphør(
                        årsaker = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
                        begrunnelse = "Aktiviteten er avsluttet",
                        opphørsdato = opphørsdato,
                    )
                }
            }

        val vedtakResponse =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, opphørId)
                .expectOkWithBody<OpphørFlyttingResponse>()
        val lagretVedtak = vedtakRepository.findById(opphørId).orElseThrow().withTypeOrThrow<OpphørFlytting>()

        assertThat(vedtakResponse.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(vedtakResponse.opphørsdato).isEqualTo(opphørsdato)
        assertThat(vedtakResponse.årsakerOpphør).containsExactly(ÅrsakOpphør.ENDRING_AKTIVITET)
        assertThat(vedtakResponse.begrunnelse).isEqualTo("Aktiviteten er avsluttet")
        assertThat(vedtakResponse.vedtaksperioder).isEmpty()
        assertThat(lagretVedtak.data.vedtaksperioder).isEmpty()
        assertThat(lagretVedtak.data.beregningsresultat.resultater).isEmpty()

        val andelerOpphør = tilkjentYtelseRepository.findByBehandlingId(opphørId)!!.andelerTilkjentYtelse
        assertThat(andelerOpphør).hasSize(1)
        assertThat(andelerOpphør.single().type).isEqualTo(TypeAndel.UGYLDIG)
        assertThat(andelerOpphør.single().beløp).isZero()
    }

    @Test
    fun `skal kunne opphøre én av to innvilgede flyttinger`() {
        val førsteFlyttedato = 1 januar 2026
        val andreFlyttedato = 8 januar 2026

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
            målgruppe {
                opprett {
                    målgruppeAAP(førsteFlyttedato, andreFlyttedato)
                }
            }
            vilkår {
                flyttingByrå(
                    flytteDato = førsteFlyttedato,
                    tilbudPriser = listOf(10000, 5000),
                    erBetalingDokumentert = true,
                )
                flyttingByrå(
                    flytteDato = andreFlyttedato,
                    tilbudPriser = listOf(8000, 3000),
                    erBetalingDokumentert = true,
                )
            }
        }

        val innvilgetVedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, behandling.id)
                .expectOkWithBody<InnvilgelseFlyttingResponse>()
        assertThat(innvilgetVedtak.beregningsresultat.resultater).hasSize(2)
        assertThat(innvilgetVedtak.beregningsresultat.resultater.map { it.fom })
            .containsExactly(førsteFlyttedato, andreFlyttedato)
        assertThat(innvilgetVedtak.beregningsresultat.resultater.map { it.beløp })
            .containsExactly(5000.toBigDecimal(), 3000.toBigDecimal())
        assertThat(tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse).hasSize(2)

        testoppsettService.settAndelerTilOkForBehandling(behandling.id)

        val opphørId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = behandling.id) {
                vedtak {
                    opphør(
                        årsaker = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
                        begrunnelse = "Den andre flyttingen gjennomføres ikke",
                        opphørsdato = andreFlyttedato,
                    )
                }
            }

        val vedtakResponse =
            kall.vedtak
                .hentVedtak(Stønadstype.FLYTTING_TSO, opphørId)
                .expectOkWithBody<OpphørFlyttingResponse>()
        val lagretVedtak = vedtakRepository.findById(opphørId).orElseThrow().withTypeOrThrow<OpphørFlytting>()

        assertThat(vedtakResponse.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(vedtakResponse.opphørsdato).isEqualTo(andreFlyttedato)
        assertThat(vedtakResponse.vedtaksperioder).hasSize(1)
        val vedtaksperiode = lagretVedtak.data.vedtaksperioder.single()
        assertThat(vedtaksperiode.fom).isEqualTo(førsteFlyttedato)
        assertThat(vedtaksperiode.tom).isEqualTo(førsteFlyttedato)

        val beregningsresultat =
            lagretVedtak.data.beregningsresultat.resultater
                .single()
        assertThat(beregningsresultat.fom).isEqualTo(førsteFlyttedato)
        assertThat(beregningsresultat.beløp).isEqualTo(5000.toBigDecimal())
        assertThat(beregningsresultat.fraTidligereVedtak).isTrue()
        val grunnlag = beregningsresultat.grunnlag as BeregningsgrunnlagFlyttebyrå
        assertThat(grunnlag.valgtTilbud.navn).isEqualTo("Syntetisk tilbud 2")
        assertThat(beregningsresultat.beløp).isEqualTo(grunnlag.valgtTilbud.pris)

        val andelerOpphør = tilkjentYtelseRepository.findByBehandlingId(opphørId)!!.andelerTilkjentYtelse
        val andelOpphør = andelerOpphør.single()
        assertThat(andelOpphør.fom).isEqualTo(førsteFlyttedato)
        assertThat(andelOpphør.tom).isEqualTo(førsteFlyttedato)
        assertThat(andelOpphør.beløp).isEqualTo(5000)
    }

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
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingByrå(
                    flytteDato = fom,
                    tilbudPriser = listOf(10000, 5000),
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
                        copy(
                            fakta =
                                byrå.copy(
                                    tilbud =
                                        byrå.tilbud.mapIndexed { index, tilbud ->
                                            if (index == 1) tilbud.copy(pris = 3000) else tilbud
                                        },
                                ),
                        )
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
        val grunnlagRevurdering = resultatRevurdering.grunnlag as BeregningsgrunnlagFlyttebyrå
        assertThat(grunnlagRevurdering.valgtTilbud.navn).isEqualTo("Syntetisk tilbud 2")
        assertThat(grunnlagRevurdering.valgtTilbud.pris).isEqualTo(3000.toBigDecimal())
        assertThat(resultatRevurdering.beløp).isEqualTo(grunnlagRevurdering.valgtTilbud.pris)

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
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingByrå(
                    flytteDato = fom,
                    tilbudPriser = listOf(10000, 5000),
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
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                flyttingByrå(
                    flytteDato = fom,
                    tilbudPriser = listOf(10000, 5000),
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
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom.plusDays(7))
                }
            }
            vilkår {
                flyttingByrå(
                    flytteDato = fom,
                    tilbudPriser = listOf(10000, 5000),
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
