package no.nav.tilleggsstonader.sak.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientMockConfig.Companion.MAPPE_ID_TILBAKEKREVING
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.Oppgavelager
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingBehandlingEndret
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingFagsysteminfoBehov
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingFagsysteminfoSvar
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingInfo
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingPeriode
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

class TilbakekrevingHendelseIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var tilbakekrevinghendelseService: TilbakekrevinghendelseService

    @Autowired
    private lateinit var oppgavelager: Oppgavelager

    @Autowired
    lateinit var vilkårsperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var læremidlerBeregnYtelseSteg: LæremidlerBeregnYtelseSteg

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var tilbakekrevingHendelseDelegate: TilbakekrevingHendelseDelegate

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    lateinit var forrigeBehandling: Saksbehandling
    lateinit var behandling: Saksbehandling
    lateinit var fagsak: Fagsak

    val publiserteHendelser = mutableListOf<ProducerRecord<String, String>>()

    @BeforeEach
    fun setUp() {
        val forrigeBehandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling =
                    behandling(
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().minusDays(2),
                    ),
                stønadstype = Stønadstype.LÆREMIDLER,
            )
        fagsak = testoppsettService.hentFagsak(forrigeBehandling.fagsakId)

        val fom = 1 januar 2025
        val tom = 31 mai 2025

        vilkårsperiodeRepository.insertAll(
            listOf(
                målgruppe(
                    behandlingId = forrigeBehandling.id,
                    fom = fom,
                    tom = tom,
                    faktaOgVurdering = faktaOgVurderingMålgruppeLæremidler(),
                ),
                aktivitet(
                    behandlingId = forrigeBehandling.id,
                    fom = fom,
                    tom = tom,
                    faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(),
                ),
            ),
        )

        læremidlerBeregnYtelseSteg.utførSteg(
            saksbehandling = behandlingRepository.finnSaksbehandling(forrigeBehandling.id),
            InnvilgelseLæremidlerRequest(
                vedtaksperioder = listOf(vedtaksperiodeDto(fom = fom, tom = tom)),
            ),
        )

        testoppsettService.ferdigstillBehandling(forrigeBehandling)

        val behandling =
            behandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                resultat = BehandlingResultat.OPPHØRT,
                vedtakstidspunkt = LocalDateTime.now().minusDays(2),
                forrigeIverksatteBehandlingId = forrigeBehandling.id,
            )
        testoppsettService.lagre(behandling)
        testoppsettService.lagOpphørVedtak(behandling)
        testoppsettService.ferdigstillBehandling(behandling)

        this.forrigeBehandling = testoppsettService.hentSaksbehandling(forrigeBehandling.id)
        this.behandling = testoppsettService.hentSaksbehandling(behandling.id)
    }

    @Test
    fun `mottar ukjent hendelsestype, gjør ingenting`() {
        val payload = mapOf("hendelsestype" to "ukjent_type")
        publiserTilbakekrevinghendelse(UUID.randomUUID().toString(), payload)

        verify(exactly = 0) { kafkaTemplate.send(any<ProducerRecord<String, String>>()) }
    }

    @Nested
    inner class FagsysteminfoBehov {
        @Test
        fun `mottar hendelsestype fagsysteminfo_behov med ukjent fagsystemId, gjør ingenting`() {
            val tilbakekrevingFagsysteminfoBehov =
                TilbakekrevingFagsysteminfoBehov(
                    eksternFagsakId = Random.nextLong(10, 10000).toString(),
                    kravgrunnlagReferanse = "10",
                    hendelseOpprettet = LocalDateTime.now(),
                    versjon = 1,
                )

            publiserTilbakekrevinghendelse(UUID.randomUUID().toString(), tilbakekrevingFagsysteminfoBehov)

            verify(exactly = 0) { kafkaTemplate.send(any<ProducerRecord<String, String>>()) }
        }

        @Test
        fun `mottar hendelsestype fagsysteminfo_behov, behandling har vedtak, publiserer andeler på topic`() {
            val key = UUID.randomUUID().toString()
            val payload =
                TilbakekrevingFagsysteminfoBehov(
                    eksternFagsakId = behandling.eksternFagsakId.toString(),
                    kravgrunnlagReferanse = behandling.eksternId.toString(),
                    hendelseOpprettet = LocalDateTime.now(),
                    versjon = 1,
                )

            publiserTilbakekrevinghendelse(key, payload)

            verify { kafkaTemplate.send(capture(publiserteHendelser)) }
            assertThat(publiserteHendelser).hasSize(1)
            val publisertHendelse = publiserteHendelser.single()
            assertThat(publisertHendelse.key()).isEqualTo(key)
            val tilbakekrevingFagsysteminfoSvar = objectMapper.readValue<TilbakekrevingFagsysteminfoSvar>(publisertHendelse.value())
            assertThat(tilbakekrevingFagsysteminfoSvar.utvidPerioder).isNotEmpty
            println(tilbakekrevingFagsysteminfoSvar)
        }
    }

    @Nested
    inner class BehandlingEndret {
        @Test
        fun `mottar to hendelsestype behandling_endret med status TIL_BEHANDLING, oppretter oppgave og db-innslag kun på første hendelse`() {
            val key = UUID.randomUUID().toString()
            val payload =
                TilbakekrevingBehandlingEndret(
                    eksternFagsakId = behandling.eksternFagsakId.toString(),
                    hendelseOpprettet = LocalDateTime.now(),
                    eksternBehandlingId = behandling.eksternId.toString(),
                    tilbakekreving =
                        TilbakekrevingInfo(
                            behandlingId = UUID.randomUUID().toString(),
                            sakOpprettet = LocalDateTime.now(),
                            varselSendt = null,
                            behandlingsstatus = TilbakekrevingBehandlingEndret.STATUS_TIL_BEHANDLING,
                            totaltFeilutbetaltBeløp = "10000",
                            saksbehandlingURL = "http://localhost",
                            fullstendigPeriode =
                                TilbakekrevingPeriode(
                                    fom = 1 januar 2025,
                                    tom = 31 mai 2025,
                                ),
                        ),
                    versjon = 1,
                )

            publiserTilbakekrevinghendelse(key, payload)

            verify(exactly = 0) { kafkaTemplate.send(capture(publiserteHendelser)) }

            val opprettetOppgave = oppgavelager.alleOppgaver().single { it.mappeId?.getOrNull() == MAPPE_ID_TILBAKEKREVING }
            assertThat(opprettetOppgave.oppgavetype).isEqualTo(Oppgavetype.BehandleSak.value)
            assertThat(opprettetOppgave.beskrivelse).contains(payload.tilbakekreving.saksbehandlingURL)

            // Verifiser at vi ikke oppretter flere oppgaver ved mottak av samme hendelse
            publiserTilbakekrevinghendelse(key, payload)
            assertThatNoException().isThrownBy {
                oppgavelager.alleOppgaver().single { it.mappeId?.getOrNull() == MAPPE_ID_TILBAKEKREVING }
            }
            assertThat(tilbakekrevinghendelseService.hentHendelserForBehandling(behandling.id)).hasSize(1)
        }
    }

    private fun publiserTilbakekrevinghendelse(
        key: String,
        payload: Any,
    ) {
        tilbakekrevingHendelseDelegate.håndter(
            ConsumerRecordUtil.lagConsumerRecord(key, objectMapper.writeValueAsString(payload)),
        )
    }
}
