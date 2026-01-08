package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.familie.prosessering.domene.Status
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTask
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.collections.map

class InnvilgeDaligReiseTsrIntegrationTest : CleanDatabaseIntegrationTest() {
    val fom = 1 september 2025
    val tom = 30 september 2025

    @Autowired
    lateinit var ytelseClient: YtelseClient

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `innvilge daglig reise med gyldig tiltak, verifiser type andel og brukers nav kontor sendt til økonomi`() {
        // For at det skal opprettes sak med stønadstype DAGLIG_REISE_TSR
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val behandlingId =
            gjennomførBehandlingsløp(
                fraJournalpost =
                    journalpost(
                        journalpostId = "1",
                        journalstatus = Journalstatus.MOTTATT,
                        dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
                        bruker = Bruker("12345678910", BrukerIdType.FNR),
                        tema = Tema.TSR.name,
                    ),
                medAktivitet = { behandlingId ->
                    lagreVilkårperiodeAktivitet(
                        behandlingId = behandlingId,
                        aktivitetType = AktivitetType.TILTAK,
                        typeAktivitet = TypeAktivitet.ENKELAMO,
                        fom = fom,
                        tom = tom,
                        faktaOgSvar =
                            FaktaOgSvarAktivitetDagligReiseTsrDto(
                                svarHarUtgifter = SvarJaNei.JA,
                            ),
                    )
                },
                medMålgruppe = { behandlingId ->
                    lagreVilkårperiodeMålgruppe(
                        behandlingId = behandlingId,
                        målgruppeType = MålgruppeType.TILTAKSPENGER,
                        fom = fom,
                        tom = tom,
                    )
                },
                medVilkår = listOf(lagreDagligReiseDto(fom = fom, tom = tom)),
            )

        // valider har fått korrekte andeler
        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandlingId)!!.andelerTilkjentYtelse

        assertThat(andeler).allMatch { it.type == TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO }
        assertThat(andeler).allMatch { it.brukersNavKontor != null }

        assertThat(taskService.finnAlleTaskerMedType(InterntVedtakTask.TYPE)).allMatch { it.status == Status.FERDIG }

        val utbetalingRecord =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .map { it.verdiEllerFeil<IverksettingDto>() }
                .single()

        // Betalende enhet skal alltid være satt for TSR/tiltaksenheten
        assertThat(utbetalingRecord.utbetalinger.flatMap { it.perioder }).allMatch { it.betalendeEnhet != null }
    }
}
