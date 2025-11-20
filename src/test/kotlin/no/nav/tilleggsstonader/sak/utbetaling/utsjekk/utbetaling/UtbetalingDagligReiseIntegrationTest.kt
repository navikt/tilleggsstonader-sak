package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.finnPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class UtbetalingDagligReiseIntegrationTest : CleanDatabaseIntegrationTest() {
    val utbetalingTopic = "tilleggsstonader.utbetaling.v1"

    @Test
    fun `utbetalingsdato i fremtiden - ingen andeler skal bli utbetalt`() {
        val reiseFramITid = lagreDagligReiseDto(fom = LocalDate.now().plusDays(1), tom = LocalDate.now().plusWeeks(1))

        gjennomførBehandlingsløp(
            medAktivitet = langtvarendeAktivitet,
            medMålgruppe = langtvarendeMålgruppe,
            medVilkår = listOf(reiseFramITid),
        )

        KafkaTestConfig.sendteMeldinger().forventAntallMeldingerPåTopic(utbetalingTopic, 0)
    }

    @Test
    fun `to andeler forrige måned, sender da én utbetaling med én periode`() {
        val forrigeMåned = YearMonth.now().minusMonths(1)
        val førsteIMåneden = forrigeMåned.atDay(1)
        val reiser =
            listOf(
                lagreDagligReiseDto(fom = førsteIMåneden, tom = førsteIMåneden),
                lagreDagligReiseDto(fom = forrigeMåned.atDay(10), tom = forrigeMåned.atDay(10)),
            )

        val behandlingId =
            gjennomførBehandlingsløp(
                medAktivitet = langtvarendeAktivitet,
                medMålgruppe = langtvarendeMålgruppe,
                medVilkår = reiser,
            )

        val utbetalinger = KafkaTestConfig.sendteMeldinger().finnPåTopic(utbetalingTopic)
        val utbetaling = utbetalinger.single().verdiEllerFeil<IverksettingDto>()

        val forventetDatoForUtbetalingsperiode = førsteIMåneden.datoEllerNesteMandagHvisLørdagEllerSøndag()

        with(utbetaling.utbetalingsgrunnlag) {
            assertThat(periodetype).isEqualTo(PeriodetypeUtbetaling.UKEDAG)
            assertThat(behandlingId).isEqualTo(behandlingId)
            with(perioder.single()) {
                assertThat(fom).isEqualTo(tom).isEqualTo(forventetDatoForUtbetalingsperiode)
            }
        }
    }

    @Test
    fun `hvis vi har én andel nå og én andel fra i tid, skal vi bare iverksette den første andelen`() {
        val reiser =
            listOf(
                lagreDagligReiseDto(fom = nå.minusDays(5), tom = nå),
                lagreDagligReiseDto(fom = nå.plusWeeks(1), tom = nå.plusWeeks(2)),
            )

        gjennomførBehandlingsløp(
            medAktivitet = langtvarendeAktivitet,
            medMålgruppe = langtvarendeMålgruppe,
            medVilkår = reiser,
        )

        val utbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .finnPåTopic(utbetalingTopic)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        val førsteUkedagDenneMåneden = YearMonth.now().atDay(1).datoEllerNesteMandagHvisLørdagEllerSøndag()

        with(utbetaling.utbetalingsgrunnlag.perioder.single()) {
            assertThat(fom).isEqualTo(tom).isEqualTo(førsteUkedagDenneMåneden)
        }
    }

    @Test
    fun `to andeler tilbake i tid med forskjellige type, skal bli to utbetalinger`() {
    }

    @Test
    fun `iverksettingId blir riktig`() {
    }

    @Test
    fun `én andel i april, én i juni, skal havne på riktig utbetalingsmåned`() {
    }

    @Test
    fun `tester på status-oppdatering`() {
    }

    @Test
    fun `Håndtering av error-status på topic`() {
    }

    private val nå: LocalDate = LocalDate.now()
    private val langtvarendeMålgruppe = fun(behandlingId: BehandlingId) =
        lagreVilkårperiodeMålgruppe(
            behandlingId,
            fom = nå.minusMonths(3),
            tom = nå.plusMonths(3),
        )
    private val langtvarendeAktivitet = fun (behandlingId: BehandlingId) =
        lagreVilkårperiodeAktivitet(
            behandlingId,
            fom = nå.minusMonths(3),
            tom = nå.plusMonths(3),
        )
}
