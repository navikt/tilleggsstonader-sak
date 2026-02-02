package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.finnPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.totrinnskontroll
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.module.kotlin.readValue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class IverksettServiceTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var iverksettService: IverksettService

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    val forrigeMåned = YearMonth.now().minusMonths(1)
    val nåværendeMåned = YearMonth.now()
    val nesteMåned = YearMonth.now().plusMonths(1)
    val nestNesteMåned = YearMonth.now().plusMonths(2)

    private fun IverksettService.iverksett(
        behandlingId: BehandlingId,
        iverksettingId: BehandlingId,
        utbetalingsdato: LocalDate,
    ) {
        this.iverksett(behandlingId, iverksettingId.id, utbetalingsdato)
    }

    @Test
    fun `skal ikke iverksette hvis resultat er avslag`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(behandling(resultat = BehandlingResultat.AVSLÅTT))

        iverksettService.iverksett(behandling.id, behandling.id, nesteMåned.atEndOfMonth())

        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
    }

    @Test
    fun `skal iverksette og oppdatere andeler med status`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, resultat = BehandlingResultat.INNVILGET))
        testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling.id))
        val tilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        iverksettService.iverksett(behandling.id, behandling.id, nesteMåned.atEndOfMonth())

        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrThrow(tilkjentYtelse.id)
        val andel = oppdatertTilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(andel.iverksetting?.iverksettingId).isEqualTo(behandling.id.id)
        assertThat(andel.statusIverksetting).isEqualTo(StatusIverksetting.SENDT)
    }

    @Nested
    inner class IverksettingFlyt {
        val fagsak = fagsak()

        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)
        val behandling2 =
            behandling(
                fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
                forrigeIverksatteBehandlingId = behandling.id,
            )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, andeler = lagAndeler(behandling))
        val tilkjentYtelse2 = tilkjentYtelse(behandlingId = behandling2.id, andeler = lagAndeler(behandling2))

        @BeforeEach
        fun setUp() {
            testoppsettService.opprettBehandlingMedFagsak(behandling)
            tilkjentYtelseRepository.insert(tilkjentYtelse)
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling.id))
            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
        }

        @Test
        fun `første behandling - første iverksetting`() {
            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)

            val iverksettingDto =
                KafkaTestConfig
                    .sendteMeldinger()
                    .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                    .single()
                    .verdiEllerFeil<IverksettingDto>()

            if (!erHelgOgFørsteEllerAndreDagIMåned()) {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
                iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned)
            } else {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
                iverksettingDto.assertUtbetalingerInneholder(forrigeMåned)
            }

            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
        }

        @Test
        fun `første behandling  - andre iverksetting`() {
            val iverksettingId = UUID.randomUUID()
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksett(behandling.id, iverksettingId, nesteMåned.atEndOfMonth())

            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            if (!erHelgOgFørsteEllerAndreDagIMåned()) {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            } else {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            }

            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            val iverksettingDto =
                KafkaTestConfig
                    .sendteMeldinger()
                    .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                    .single { it.key() == iverksettingId.toString() }
                    .verdiEllerFeil<IverksettingDto>()

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned, nesteMåned)
        }

        @Test
        fun `andre behandling - første iverksetting - skal bruke behandling2 som iverksettingId`() {
            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(tilkjentYtelse2)
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling2.id))
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            val iverksettingDto =
                KafkaTestConfig
                    .sendteMeldinger()
                    .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                    .single { it.key() == behandling2.id.toString() }
                    .verdiEllerFeil<IverksettingDto>()

            if (!erHelgOgFørsteEllerAndreDagIMåned()) {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
                iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned)
            } else {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
                iverksettingDto.assertUtbetalingerInneholder(forrigeMåned)
            }
        }

        @Test
        fun `andre behandling - første iverksetting med 2 iverksettinger`() {
            val iverksettingIdBehandling1 = UUID.randomUUID()
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksett(behandling.id, iverksettingIdBehandling1, nesteMåned.atEndOfMonth())

            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(tilkjentYtelse2)
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling2.id))
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            val iverksettingDto =
                KafkaTestConfig
                    .sendteMeldinger()
                    .finnPåTopic(kafkaTopics.utbetaling)
                    .single { it.key() == behandling2.id.toString() }
                    .verdiEllerFeil<IverksettingDto>()

            if (!erHelgOgFørsteEllerAndreDagIMåned()) {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
                iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned)
            } else {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
                iverksettingDto.assertUtbetalingerInneholder(forrigeMåned)
            }
        }

        @Test
        fun `andre behandling  - andre iverksetting`() {
            val iverksettingId = UUID.randomUUID()

            oppdaterAndelerTilOk(behandling)

            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(tilkjentYtelse2)
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling2.id))

            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)
            oppdaterAndelerTilOk(behandling2)
            iverksettService.iverksett(behandling2.id, iverksettingId, nesteMåned.atEndOfMonth())

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling2.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            if (!erHelgOgFørsteEllerAndreDagIMåned()) {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling2.id)
            } else {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            }

            val iverksettingDto =
                KafkaTestConfig
                    .sendteMeldinger()
                    .finnPåTopic(kafkaTopics.utbetaling)
                    .single { it.key() == iverksettingId.toString() }
                    .verdiEllerFeil<IverksettingDto>()

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned, nesteMåned)

            assertThat(iverksettingDto.behandlingId).isEqualTo(hentEksternBehandlingId(behandling2))
        }

        @Test
        fun `andre behandling kun med 0-beløp - skal ikke sende noen andeler`() {
            oppdaterAndelerTilOk(behandling)
            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandling2.id,
                    lagAndel(behandling2, forrigeMåned, beløp = 0),
                ),
            )
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling2.id))
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)

            val iverksettingDto =
                KafkaTestConfig
                    .sendteMeldinger()
                    .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                    .single { it.key() == behandling2.id.toString() }
                    .verdiEllerFeil<IverksettingDto>()

            assertThat(iverksettingDto.utbetalinger.single().perioder).isEmpty()
        }

        @Test
        fun `skal feile hvis forrige iverksetting ikke er ferdigstilt`() {
            val iverksettingId = UUID.randomUUID()
            assertThatThrownBy {
                iverksettService.iverksett(behandling.id, iverksettingId, nåværendeMåned.atEndOfMonth())
            }.hasMessageContaining("det finnes tidligere andeler med annen status enn OK/UBEHANDLET")
        }

        @Test
        fun `skal markere andeler fra forrige behandling som UAKTUELL`() {
            oppdaterAndelerTilOk(behandling)
            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandling2.id,
                    lagAndel(behandling2, forrigeMåned, beløp = 100),
                ),
            )
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling2.id))
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)

            if (!erHelgOgFørsteEllerAndreDagIMåned()) {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            } else {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)
            }
        }

        @Test
        fun `skal markere andeler fra forrige behandling med status VENTER_PÅ_SATSENDRING som UAKTUELL`() {
            oppdaterAndelerTilOk(behandling)
            val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
            val treMndFrem = YearMonth.now().plusMonths(3)

            // Legger til andel med status VENTER_PÅ_SATSENDRING
            tilkjentYtelseRepository.update(
                tilkjentYtelse.copy(
                    andelerTilkjentYtelse =
                        tilkjentYtelse.andelerTilkjentYtelse.plus(
                            lagAndel(
                                behandling,
                                treMndFrem,
                                statusIverksetting = StatusIverksetting.VENTER_PÅ_SATS_ENDRING,
                            ),
                        ),
                ),
            )

            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandling2.id,
                    lagAndel(behandling2, forrigeMåned, beløp = 100),
                ),
            )
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling2.id))
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)
            andeler.forMåned(treMndFrem).assertHarStatusOgId(StatusIverksetting.UAKTUELL)

            if (!erHelgOgFørsteEllerAndreDagIMåned()) {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            } else {
                andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)
            }
        }

        @Test
        fun `skal feile hvis en andel fra forrige behandling er sendt til iverksetting med ikke kvittert OK`() {
            testoppsettService.lagre(behandling2)

            assertThatThrownBy { iverksettService.iverksettBehandlingFørsteGang(behandling2.id) }
                .hasMessageContaining("Andeler fra forrige behandling er sendt til iverksetting men ikke kvittert OK. Prøv igjen senere.")
        }
    }

    @Nested
    inner class HåndteringAvAndelSomSkalUtbetalesUlikeDager {
        val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling.id))
        }

        @Test
        fun `Skal kun iverksette andeler innenfor en måned som gjelder for gitt utbetalingsdato`() {
            val andel1 =
                andelTilkjentYtelse(
                    kildeBehandlingId = behandling.id,
                    fom = nåværendeMåned.atDay(1).datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    utbetalingsdato = nåværendeMåned.atDay(1),
                    type = TypeAndel.LÆREMIDLER_AAP,
                )
            val andel2 =
                andelTilkjentYtelse(
                    kildeBehandlingId = behandling.id,
                    fom = nesteMåned.atDay(1).datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    type = TypeAndel.LÆREMIDLER_AAP,
                )
            val andel3 =
                andelTilkjentYtelse(
                    kildeBehandlingId = behandling.id,
                    fom = nesteMåned.atDay(15).datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    type = TypeAndel.LÆREMIDLER_AAP,
                )
            tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id, andel1, andel2, andel3))

            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
            with(hentAndeler(behandling)) {
                single { it.id == andel1.id }.assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
                single { it.id == andel2.id }.assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
                single { it.id == andel3.id }.assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            }

            oppdaterAndelerTilOk(behandling, StatusIverksetting.OK)

            // iverksetter med utbetalingsdato for andel2, som er den samme måneden som andel3, men andel 3 er senere den måneden
            iverksettService.iverksett(behandling.id, andel2.id, utbetalingsdato = andel2.fom)
            with(hentAndeler(behandling)) {
                single { it.id == andel1.id }.assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
                single { it.id == andel2.id }.assertHarStatusOgId(StatusIverksetting.SENDT, andel2.id)
                single { it.id == andel3.id }.assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            }
        }
    }

    @Nested
    inner class IverksettFlytMedAndelerSomIkkeSkalUtbetales {
        val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)

        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
            testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling.id))
        }

        @Test
        fun `skal ikke sende andeler som har status=VENTER_PÅ_SATS_ENDRING som skal satsjusteres før de blir iverksatte`() {
            val tilkjentYtelse =
                tilkjentYtelse(
                    behandlingId = behandling.id,
                    lagAndel(
                        behandling = behandling,
                        måned = forrigeMåned,
                        type = TypeAndel.LÆREMIDLER_AAP,
                    ),
                    lagAndel(
                        behandling = behandling,
                        måned = nesteMåned,
                        type = TypeAndel.LÆREMIDLER_AAP,
                    ),
                    lagAndel(
                        behandling = behandling,
                        måned = nestNesteMåned,
                        type = TypeAndel.LÆREMIDLER_AAP,
                        statusIverksetting = StatusIverksetting.VENTER_PÅ_SATS_ENDRING,
                    ),
                )
            tilkjentYtelseRepository.insert(tilkjentYtelse)

            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
            with(hentAndeler(behandling)) {
                forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
                forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
                forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
            }
            oppdaterAndelerTilOk(behandling, StatusIverksetting.OK)

            val iverksettingId = UUID.randomUUID()
            iverksettService.iverksett(behandling.id, iverksettingId, nestNesteMåned.plusMonths(1).atEndOfMonth())
            with(hentAndeler(behandling)) {
                forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
                forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
                forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
            }
        }

        @Test
        fun `skal ikke iverksette noen perioder hvis man innvilget en periode bak i tiden som fortsatt status=VENTER_PÅ_SATS_ENDRING`() {
            val tilkjentYtelse =
                tilkjentYtelse(
                    behandlingId = behandling.id,
                    lagAndel(
                        behandling = behandling,
                        måned = forrigeMåned,
                        statusIverksetting = StatusIverksetting.VENTER_PÅ_SATS_ENDRING,
                        type = TypeAndel.LÆREMIDLER_AAP,
                    ),
                )
            tilkjentYtelseRepository.insert(tilkjentYtelse)
            iverksettService.iverksettBehandlingFørsteGang(behandling.id)

            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

            with(hentAndeler(behandling)) {
                forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
                assertThat(this).hasSize(1)
            }
        }
    }

    private fun hentEksternBehandlingId(behandling: Behandling) = testoppsettService.hentSaksbehandling(behandling.id).eksternId.toString()

    private fun hentAndeler(behandling: Behandling): Set<AndelTilkjentYtelse> =
        tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse

    private fun Collection<AndelTilkjentYtelse>.forMåned(yearMonth: YearMonth): AndelTilkjentYtelse {
        val dato = yearMonth.atDay(1)
        return this.single {
            val datoEllerNesteMandag =
                if (it.satstype == Satstype.DAG) dato.datoEllerNesteMandagHvisLørdagEllerSøndag() else dato
            it.fom == datoEllerNesteMandag
        }
    }

    fun AndelTilkjentYtelse.assertHarStatusOgId(
        statusIverksetting: StatusIverksetting,
        iverksettingId: BehandlingId?,
    ) {
        assertHarStatusOgId(statusIverksetting, iverksettingId?.id)
    }

    fun AndelTilkjentYtelse.assertHarStatusOgId(
        statusIverksetting: StatusIverksetting,
        iverksettingId: UUID? = null,
    ) {
        assertThat(this.statusIverksetting).isEqualTo(statusIverksetting)
        assertThat(this.iverksetting?.iverksettingId).isEqualTo(iverksettingId)
    }

    private fun oppdaterAndelerTilOk(
        behandling: Behandling,
        statusIverksetting: StatusIverksetting = StatusIverksetting.OK,
    ) {
        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse
        val oppdaterteAndeler =
            andeler
                .filter { it.statusIverksetting == StatusIverksetting.SENDT }
                .map { it.copy(statusIverksetting = statusIverksetting) }
        andelTilkjentYtelseRepository.updateAll(oppdaterteAndeler)
    }

    private fun lagAndeler(behandling: Behandling) =
        arrayOf(
            lagAndel(behandling, forrigeMåned),
            lagAndel(behandling, nåværendeMåned),
            lagAndel(behandling, nesteMåned),
            lagAndel(behandling, nestNesteMåned),
        )

    private fun lagAndel(
        behandling: Behandling,
        måned: YearMonth,
        beløp: Int = 10,
        statusIverksetting: StatusIverksetting = StatusIverksetting.UBEHANDLET,
        type: TypeAndel = TypeAndel.TILSYN_BARN_AAP,
    ): AndelTilkjentYtelse {
        val fom = måned.atDay(1).datoEllerNesteMandagHvisLørdagEllerSøndag()
        return andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            fom = fom,
            tom = fom,
            beløp = beløp,
            statusIverksetting = statusIverksetting,
            type = type,
        )
    }

    private fun IverksettingDto.assertUtbetalingerInneholder(vararg måned: YearMonth) {
        assertThat(utbetalinger.single().perioder.map { YearMonth.from(it.fom) })
            .containsExactlyInAnyOrder(*måned)
    }

    private fun erHelgOgFørsteEllerAndreDagIMåned(): Boolean {
        val dagensDato = LocalDate.now()
        val dagIMåned = dagensDato.dayOfMonth
        val dagIUken = dagensDato.dayOfWeek
        return (dagIMåned == 1 || dagIMåned == 2) &&
            (dagIUken == DayOfWeek.SATURDAY || dagIUken == DayOfWeek.SUNDAY)
    }
}
