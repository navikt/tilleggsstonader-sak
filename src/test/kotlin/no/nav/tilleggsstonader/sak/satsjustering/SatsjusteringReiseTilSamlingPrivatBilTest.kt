package no.nav.tilleggsstonader.sak.satsjustering

import io.mockk.clearMocks
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.sats.satser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SatsjusteringReiseTilSamlingPrivatBilTest(
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var faktaGrunnlagService: FaktaGrunnlagService

    @Autowired
    lateinit var satsPrivatBilProvider: SatsPrivatBilProvider

    // Perioden ligger etter siste bekreftede sats, uavhengig av når nye satser blir bekreftet
    private val sisteBekreftedeÅr = satser.filter { it.bekreftet }.maxOf { it.fom.year }
    private val fom = 1 januar (sisteBekreftedeÅr + 1)
    private val tom = 31 januar (sisteBekreftedeÅr + 1)

    @AfterEach
    fun resetMock() {
        clearMocks(satsPrivatBilProvider)
    }

    @Test
    fun `skal justere sats i revurdering når satsen for privatbil-reise blir bekreftet`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        privatBilReiseTilSamling(fom, tom)
                    }
                }
            }

        val andelerFørSatsjustering =
            tilkjentYtelseRepository
                .findByBehandlingId(behandlingContext.behandlingId)!!
                .andelerTilkjentYtelse
        assertThat(andelerFørSatsjustering).hasSize(1)
        assertThat(andelerFørSatsjustering.single().statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)

        mockBekreftetSats()

        val behandlingerForSatsjustering =
            medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
                kall.satsjustering.satsjustering(Stønadstype.REISE_TIL_SAMLING_TSO)
            }

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        assertThat(behandlingerForSatsjustering).containsExactly(behandlingContext.behandlingId)

        val satsjusteringBehandling = behandlingRepository.finnSisteIverksatteBehandling(behandlingContext.fagsakId)!!
        assertThat(satsjusteringBehandling.id).isNotEqualTo(behandlingContext.behandlingId)
        assertThat(satsjusteringBehandling.forrigeIverksatteBehandlingId).isEqualTo(behandlingContext.behandlingId)
        assertThat(satsjusteringBehandling.behandlingMetode).isEqualTo(BehandlingMetode.BATCH)
        assertThat(satsjusteringBehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        val grunnlagSatsjusteringBehandling = faktaGrunnlagService.hentGrunnlagsdata(satsjusteringBehandling.id)
        val grunnlagForrigeBehandling = faktaGrunnlagService.hentGrunnlagsdata(behandlingContext.behandlingId)
        assertThat(grunnlagSatsjusteringBehandling).isEqualTo(grunnlagForrigeBehandling)

        val andelerEtterSatsjustering =
            tilkjentYtelseRepository
                .findByBehandlingId(satsjusteringBehandling.id)!!
                .andelerTilkjentYtelse
        assertThat(andelerEtterSatsjustering).noneMatch {
            it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING
        }
    }

    private fun mockBekreftetSats() {
        val bekreftedeSatser = satsPrivatBilProvider.alleSatser.filter { it.bekreftet }
        val ubekreftetSats = satsPrivatBilProvider.alleSatser.first { !it.bekreftet }
        val nyBekreftetSats =
            ubekreftetSats.copy(
                bekreftet = true,
                beløp = 10.toBigDecimal(),
            )

        every {
            satsPrivatBilProvider.alleSatser
        } returns bekreftedeSatser + nyBekreftetSats
    }

    @Test
    fun `andel for privatbil-reise skal vente på satsendring når satsen ikke er bekreftet`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        privatBilReiseTilSamling(fom, tom)
                    }
                }
            }

        val andeler =
            tilkjentYtelseRepository
                .findByBehandlingId(behandlingContext.behandlingId)!!
                .andelerTilkjentYtelse

        assertThat(andeler).hasSize(1)
        assertThat(andeler.single().statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
    }
}
