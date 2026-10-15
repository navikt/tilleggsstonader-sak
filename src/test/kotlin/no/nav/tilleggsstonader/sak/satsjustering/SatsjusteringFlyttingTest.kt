package no.nav.tilleggsstonader.sak.satsjustering

import io.mockk.clearMocks
import io.mockk.every
import no.nav.familie.prosessering.domene.Status
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.sats.satser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SatsjusteringFlyttingTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var faktaGrunnlagService: FaktaGrunnlagService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var satsPrivatBilProvider: SatsPrivatBilProvider

    private val flytteÅr = LocalDate.now().year
    private val flytteDato = 1 januar flytteÅr

    @AfterEach
    fun resetMock() {
        clearMocks(satsPrivatBilProvider)
    }

    @Test
    fun `skal justere sats for flytting`() {
        val stønadstype = Stønadstype.FLYTTING_TSO
        val ident = FnrGenerator.generer()
        gjørÅretsSatsUbekreftet()
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = stønadstype,
                identer = setOf(PersonIdent(ident = ident)),
            )

        opprettOgTilordneOppgaveForBehandling(behandling.id)
        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id) {
            målgruppe {
                opprett {
                    målgruppeAAP(flytteDato, flytteDato)
                }
            }
            vilkår {
                flyttingEgenKjøring(
                    flytteDato = flytteDato,
                    avstandEnVei = 100,
                )
            }
        }

        val andelerFørSatsjustering =
            tilkjentYtelseRepository
                .findByBehandlingId(behandling.id)!!
                .andelerTilkjentYtelse
        assertThat(andelerFørSatsjustering).hasSize(1)
        assertThat(andelerFørSatsjustering.single().beløp).isNotEqualTo(1000)
        assertThat(andelerFørSatsjustering.single().statusIverksetting)
            .isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        bekreftNySats()

        val behandlingerForSatsjustering =
            medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
                kall.satsjustering.satsjustering(stønadstype)
            }

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        assertThat(behandlingerForSatsjustering).containsExactly(behandling.id)
        assertThat(taskService.finnAlleTaskerMedType(SatsjusteringTask.TYPE))
            .filteredOn { it.payload == behandling.id.toString() }
            .singleElement()
            .extracting { it.status }
            .isEqualTo(Status.FERDIG)
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)

        val satsjusteringBehandling = behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsakId)!!
        assertThat(satsjusteringBehandling.id).isNotEqualTo(behandling.id)
        assertThat(satsjusteringBehandling.forrigeIverksatteBehandlingId).isEqualTo(behandling.id)
        assertThat(satsjusteringBehandling.behandlingMetode).isEqualTo(BehandlingMetode.BATCH)
        assertThat(satsjusteringBehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(satsjusteringBehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        val grunnlagEtterSatsjustering = faktaGrunnlagService.hentGrunnlagsdata(satsjusteringBehandling.id)
        val grunnlagFørSatsjustering = faktaGrunnlagService.hentGrunnlagsdata(behandling.id)
        assertThat(grunnlagEtterSatsjustering).isEqualTo(grunnlagFørSatsjustering)

        val andelerEtterSatsjustering =
            tilkjentYtelseRepository
                .findByBehandlingId(satsjusteringBehandling.id)!!
                .andelerTilkjentYtelse
        assertThat(andelerEtterSatsjustering).hasSize(1)
        assertThat(andelerEtterSatsjustering.single().beløp).isEqualTo(1000)
        assertThat(andelerEtterSatsjustering)
            .noneMatch { it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING }
    }

    private fun bekreftNySats() {
        val satserForÅret = satsPrivatBilProvider.alleSatser
        val bekreftedeSatser = satserForÅret.filter { it.bekreftet }
        val ubekreftetSats = satserForÅret.single { !it.bekreftet && it.fom.year == flytteÅr }
        val nyBekreftetSats =
            ubekreftetSats.copy(
                bekreftet = true,
                beløp = 10.toBigDecimal(),
            )

        every {
            satsPrivatBilProvider.alleSatser
        } returns bekreftedeSatser + nyBekreftetSats
    }

    private fun gjørÅretsSatsUbekreftet() {
        every {
            satsPrivatBilProvider.alleSatser
        } returns satser.map { if (it.fom.year == flytteÅr) it.copy(bekreftet = false) else it }
    }
}
