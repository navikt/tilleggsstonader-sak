package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandling.opprettelse.ForenkletBehandlingstype
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeRepository
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.erFørNåværendeUke
import no.nav.tilleggsstonader.sak.util.finnNesteSøndag
import no.nav.tilleggsstonader.sak.util.iDagHvisMandagEllerForrigeMandag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate
import kotlin.random.Random

class KjørelisteManuellRegistreringControllerTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    private lateinit var avklartKjørtUkeRepository: AvklartKjørtUkeRepository

    @Test
    fun `skal være mulig å manuelt registrere en kjøreliste som blir utbetalt`() {
        val fom = 5 januar 2026
        val tom = 11 januar 2026
        val (revurderingId, fagsakId) = opprettRammevedtakOgRevurdering(fom, tom)

        val kjørelisteOversikt = kall.privatBil.hentKjørelisteOversikt(revurderingId)

        assertThat(kjørelisteOversikt.tilgjengeligeReiser).hasSize(1)
        assertThat(kjørelisteOversikt.kjørelisterLagretIBehandling).isEmpty()

        val reisedager = lagKjørteDagerForUke(fom = 5 januar 2026, tom = 11 januar 2026, antallKjørteDager = 2)

        val lagreRequest =
            LagreManuellKjørelisteRequest(
                journalpostId = journalpostId(),
                reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                begrunnelse = null,
                reisedager = reisedager,
            )

        kall.privatBil.lagreManuellKjøreliste(
            revurderingId,
            lagreRequest,
        )

        // Sjekk at riktig kjøreliste ble lagret ned
        val alleKjørelisterPåFagsak = kjørelisteRepository.findByFagsakId(fagsakId)
        assertThat(alleKjørelisterPåFagsak).hasSize(1)
        assertThat(alleKjørelisterPåFagsak.single().data.fom).isEqualTo(fom)
        assertThat(alleKjørelisterPåFagsak.single().data.tom).isEqualTo(tom)

        kall.steg.ferdigstill(revurderingId, StegController.FerdigstillStegRequest(StegType.REGISTRER_KJØRELISTE))

        // Kjørelister skal avklares ved ferdigstilling av steg
        val avklarteUker = avklartKjørtUkeRepository.findByBehandlingId(revurderingId)
        assertThat(avklarteUker).hasSize(1)
        assertThat(avklarteUker.single().dager.minOf { it.dato }).isEqualTo(fom)
        assertThat(avklarteUker.single().dager.maxOf { it.dato }).isEqualTo(tom)

        val revurdering = behandlingService.hentBehandling(revurderingId)

        gjennomførKjørelisteBehandling(revurdering)

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
    }

    @Nested
    inner class ValideringVedLagring {
        @Test
        fun `skal ikke være mulig å sende inn kjørelister fremover i tid`() {
            val dagensDato = LocalDate.now()
            val fom = dagensDato.minusMonths(1)
            val tom = dagensDato.plusMonths(1)

            val (revurderingId) = opprettRammevedtakOgRevurdering(fom, tom)

            val kjørelisteOversikt = kall.privatBil.hentKjørelisteOversikt(revurderingId)

            // Skal kun hente uker bakover i tid
            kjørelisteOversikt.tilgjengeligeReiser.forEach {
                it.uker.forEach { uke ->
                    assertThat(uke.fom.tilUkeIÅr().erFørNåværendeUke()).isTrue
                }
            }

            val mandag = dagensDato.iDagHvisMandagEllerForrigeMandag()

            val reisedager = lagKjørteDagerForUke(fom = mandag, tom = mandag.finnNesteSøndag(), antallKjørteDager = 2)

            val lagreRequest =
                LagreManuellKjørelisteRequest(
                    journalpostId = journalpostId(),
                    reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                    begrunnelse = null,
                    reisedager = reisedager,
                )

            kall.privatBil.apiRespons
                .lagreManuellKjøreliste(
                    revurderingId,
                    lagreRequest,
                ).expectProblemDetail(
                    HttpStatus.BAD_REQUEST,
                    "Kan ikke registrere kjøreliste for dager som er fremover i tid",
                )
        }

        @Test
        fun `skal ikke være mulig å registrere kjørelister for uker som alt er innsendt`() {
            val fom = 5 januar 2026
            val tom = 18 januar 2026

            val (revurderingId) =
                opprettRammevedtakOgRevurdering(
                    fom = fom,
                    tom = tom,
                    skalSendeInnKjørelisteForFørsteUka = true,
                )

            val kjørelisteOversikt = kall.privatBil.hentKjørelisteOversikt(revurderingId)

            val innsendteUker =
                kjørelisteOversikt.tilgjengeligeReiser
                    .single()
                    .uker
                    .filter { it.innsendtTidligere }
            assertThat(innsendteUker).hasSize(1)
            assertThat(innsendteUker.single().fom).isEqualTo(fom)

            val ikkeInnsendteUker =
                kjørelisteOversikt.tilgjengeligeReiser
                    .single()
                    .uker
                    .filter { !it.innsendtTidligere }
            assertThat(ikkeInnsendteUker).hasSize(1)

            // Sender inn for uke som alt er registrert
            kall.privatBil.apiRespons
                .lagreManuellKjøreliste(
                    revurderingId,
                    LagreManuellKjørelisteRequest(
                        journalpostId = journalpostId(),
                        reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                        begrunnelse = null,
                        reisedager = lagKjørteDagerForUke(fom = fom, tom = 11 januar 2026, antallKjørteDager = 2),
                    ),
                ).expectProblemDetail(
                    forventetStatus = HttpStatus.BAD_REQUEST,
                    forventetDetail = "Innsendte dager overlapper med tidligere innsendte kjørelister",
                )
        }

        @Test
        fun `skal ikke være mulig å sende inn kjøreliste hvis rammevedtak ikke finnes`() {
            val fom = 1 januar 2026
            val tom = 31 januar 2026

            val (revurderingId) = opprettRammevedtakOgRevurdering(fom, tom)

            val reisedager = lagKjørteDagerForUke(fom = fom, tom = tom, antallKjørteDager = 2)

            val lagreRequest =
                LagreManuellKjørelisteRequest(
                    journalpostId = journalpostId(),
                    reiseId = ReiseId.random(),
                    begrunnelse = null,
                    reisedager = reisedager,
                )

            kall.privatBil.apiRespons
                .lagreManuellKjøreliste(
                    revurderingId,
                    lagreRequest,
                ).expectProblemDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Fant ikke rammevedtak for reise",
                )
        }

        @Test
        fun `skal ikke kunne sende inn kjøreliste utenfor rammevedtaket`() {
            val fom = 5 januar 2026
            val tom = 11 januar 2026

            val (revurderingId) = opprettRammevedtakOgRevurdering(fom, tom)

            val reiseId =
                kall.privatBil
                    .hentKjørelisteOversikt(revurderingId)
                    .tilgjengeligeReiser
                    .single()
                    .reiseId

            val reisedager = lagKjørteDagerForUke(fom = 5 januar 2026, tom = 18 januar 2026, antallKjørteDager = 2)

            val lagreRequest =
                LagreManuellKjørelisteRequest(
                    journalpostId = journalpostId(),
                    reiseId = reiseId,
                    begrunnelse = null,
                    reisedager = reisedager,
                )

            kall.privatBil.apiRespons
                .lagreManuellKjøreliste(
                    revurderingId,
                    lagreRequest,
                ).expectProblemDetail(
                    HttpStatus.BAD_REQUEST,
                    "Perioden for innsendt kjøreliste er utenfor rammevedtaket",
                )
        }

        @Test
        fun `skal ikke kunne sende inn ufullstendige uker`() {
            val fom = 5 januar 2026
            val tom = 11 januar 2026

            val (revurderingId) = opprettRammevedtakOgRevurdering(fom, tom)

            val reiseId =
                kall.privatBil
                    .hentKjørelisteOversikt(revurderingId)
                    .tilgjengeligeReiser
                    .single()
                    .reiseId

            val reisedager = lagKjørteDagerForUke(fom = 5 januar 2026, tom = 6 januar 2026, antallKjørteDager = 2)

            val lagreRequest =
                LagreManuellKjørelisteRequest(
                    journalpostId = journalpostId(),
                    reiseId = reiseId,
                    begrunnelse = null,
                    reisedager = reisedager,
                )

            kall.privatBil.apiRespons
                .lagreManuellKjøreliste(
                    revurderingId,
                    lagreRequest,
                ).expectProblemDetail(
                    HttpStatus.BAD_REQUEST,
                    "Uke 2 er sendt inn ufullstendig.",
                )
        }
    }

    @Nested
    inner class Henlegg {
        @Test
        fun `skal slette nye kjørelister og avklarte uker ved henleggelse`() {
            val fom = 5 januar 2026
            val tom = 11 januar 2026

            val (revurderingId) = opprettRammevedtakOgRevurdering(fom, tom)

            val kjørelisteOversikt = kall.privatBil.hentKjørelisteOversikt(revurderingId)

            val kjørelisteId =
                kall.privatBil.lagreManuellKjøreliste(
                    revurderingId,
                    LagreManuellKjørelisteRequest(
                        journalpostId = journalpostId(),
                        reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                        begrunnelse = null,
                        reisedager = lagKjørteDagerForUke(fom = fom, tom = tom, antallKjørteDager = 2),
                    ),
                )

            kall.steg.ferdigstill(revurderingId, StegController.FerdigstillStegRequest(StegType.REGISTRER_KJØRELISTE))

            kall.behandling.henlegg(
                revurderingId,
                HenlagtDto(
                    årsak = HenlagtÅrsak.FEILREGISTRERT,
                    begrunnelse = "Begrunnelse",
                ),
            )

            val kjørelisterLagretIBehandling = kjørelisteRepository.findById(kjørelisteId)
            assertThat(kjørelisterLagretIBehandling).isEmpty()

            val avklarteUker = avklartKjørtUkeRepository.findByBehandlingId(revurderingId)
            assertThat(avklarteUker).isEmpty()
        }

        @Test
        fun `skal beholde eksisterende kjørelister og slette nye ved henleggelse`() {
            val fom = 5 januar 2026
            val tom = 18 januar 2026

            val (revurderingId, fagsakId) =
                opprettRammevedtakOgRevurdering(
                    fom,
                    tom,
                    skalSendeInnKjørelisteForFørsteUka = true,
                )

            val kjørelisteOversikt = kall.privatBil.hentKjørelisteOversikt(revurderingId)

            // Manuelt registrer uke 2
            val kjørelisteId =
                kall.privatBil.lagreManuellKjøreliste(
                    revurderingId,
                    LagreManuellKjørelisteRequest(
                        journalpostId = journalpostId(),
                        reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                        begrunnelse = null,
                        reisedager = lagKjørteDagerForUke(fom = 12 januar 2026, tom = tom, antallKjørteDager = 2),
                    ),
                )

            kall.steg.ferdigstill(revurderingId, StegController.FerdigstillStegRequest(StegType.REGISTRER_KJØRELISTE))

            kall.behandling.henlegg(
                revurderingId,
                HenlagtDto(
                    årsak = HenlagtÅrsak.FEILREGISTRERT,
                    begrunnelse = "Begrunnelse",
                ),
            )

            val kjørelisterLagretIBehandling =
                kjørelisteRepository.findByFagsakId(fagsakId)
            assertThat(kjørelisterLagretIBehandling).hasSize(1)
            assertThat(kjørelisterLagretIBehandling.filter { it.id == kjørelisteId }).isEmpty()

            val avklarteUker = avklartKjørtUkeRepository.findByBehandlingId(revurderingId)
            assertThat(avklarteUker.filter { it.kjørelisteId == kjørelisteId }).isEmpty()
            assertThat(avklarteUker).hasSize(1)
        }
    }

    data class RevurderingContext(
        val revurderingId: BehandlingId,
        val fagsakId: FagsakId,
    )

    private fun opprettRammevedtakOgRevurdering(
        fom: LocalDate,
        tom: LocalDate,
        skalSendeInnKjørelisteForFørsteUka: Boolean = false,
    ): RevurderingContext {
        val førstegangsBehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                if (skalSendeInnKjørelisteForFørsteUka) {
                    sendInnKjøreliste {
                        periode = Datoperiode(fom, fom.finnNesteSøndag())
                        kjørteDager =
                            listOf(
                                KjørtDag(dato = fom, parkeringsutgift = 50),
                            )
                    }
                }
            }

        if (skalSendeInnKjørelisteForFørsteUka) {
            val manuellKjørelisteBehandling =
                testoppsettService.hentBehandlinger(førstegangsBehandlingContext.fagsakId).single {
                    it.type == BehandlingType.KJØRELISTE && it.behandlingMetode == BehandlingMetode.MANUELL
                }

            gjennomførKjørelisteBehandling(manuellKjørelisteBehandling)
        }

        val revurderingId =
            opprettRevurdering(opprettBehandlingDto(fagsakId = førstegangsBehandlingContext.fagsakId))

        return RevurderingContext(
            revurderingId = revurderingId,
            fagsakId = førstegangsBehandlingContext.fagsakId,
        )
    }

    private fun lagKjørteDagerForUke(
        fom: LocalDate,
        tom: LocalDate,
        antallKjørteDager: Int,
    ): List<KjørelisteDag> =
        Datoperiode(fom, tom).alleDatoer().mapIndexed { index, dato ->
            KjørelisteDag(
                dato = dato,
                harKjørt = index < antallKjørteDager,
                parkeringsutgift = null,
            )
        }

    private fun opprettBehandlingDto(fagsakId: FagsakId) =
        OpprettBehandlingDto(
            fagsakId = fagsakId,
            forenkletBehandlingstype = ForenkletBehandlingstype.KJØRELISTE,
            årsak = BehandlingÅrsak.REGISTRER_KJØRELISTE_FOR_BRUKER,
            nyeOpplysningerMetadata = null,
            kravMottatt = LocalDate.now(),
        )

    private fun journalpostId() = Random.nextInt().toString()
}
