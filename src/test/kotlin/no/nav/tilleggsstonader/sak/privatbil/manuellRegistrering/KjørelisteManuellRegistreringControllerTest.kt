package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.dato.januar
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
import no.nav.tilleggsstonader.sak.util.finnNesteSøndag
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
    inner class SlettManuellKjøreliste {
        @Test
        fun `skal kunne slette en kjøreliste og tilhørende avklart uke som ble lagret manuelt i denne behandlingen`() {
            val fom = 5 januar 2026
            val tom = 18 januar 2026
            val (revurderingId, fagsakId) = opprettRammevedtakOgRevurdering(fom, tom)

            val reiseId =
                kall.privatBil
                    .hentKjørelisteOversikt(revurderingId)
                    .tilgjengeligeReiser
                    .single()
                    .reiseId

            val kjørelisteIdSomSkalSlettes =
                kall.privatBil
                    .lagreManuellKjøreliste(
                        behandlingId = revurderingId,
                        request =
                            LagreManuellKjørelisteRequest(
                                journalpostId = journalpostId(),
                                reiseId = reiseId,
                                begrunnelse = null,
                                reisedager =
                                    lagKjørteDagerForUke(
                                        fom = 5 januar 2026,
                                        tom = 11 januar 2026,
                                        antallKjørteDager = 2,
                                    ),
                            ),
                    ).kjørelisteId

            val kjørelisteIdSomBeholdes =
                kall.privatBil
                    .lagreManuellKjøreliste(
                        behandlingId = revurderingId,
                        request =
                            LagreManuellKjørelisteRequest(
                                journalpostId = journalpostId(),
                                reiseId = reiseId,
                                begrunnelse = null,
                                reisedager =
                                    lagKjørteDagerForUke(
                                        fom = 12 januar 2026,
                                        tom = 18 januar 2026,
                                        antallKjørteDager = 2,
                                    ),
                            ),
                    ).kjørelisteId

            // Ferdigstiller steget slik at avklarte uker opprettes
            kall.steg.ferdigstill(revurderingId, StegController.FerdigstillStegRequest(StegType.REGISTRER_KJØRELISTE))
            kall.steg.reset(revurderingId, StegController.ResetStegRequest(StegType.REGISTRER_KJØRELISTE))

            kall.privatBil.slettManuellKjøreliste(revurderingId, kjørelisteIdSomSkalSlettes)

            // Avklarte uker som ble opprettet for kjørelisten skal ha blitt slettet
            val avklarteUker = avklartKjørtUkeRepository.findByBehandlingId(revurderingId)
            assertThat(avklarteUker).hasSize(1)
            assertThat(avklarteUker.single().kjørelisteId).isEqualTo(kjørelisteIdSomBeholdes)

            val kjørelister = kjørelisteRepository.findByFagsakId(fagsakId)

            assertThat(kjørelister).hasSize(1)
            assertThat(kjørelister.single().id).isEqualTo(kjørelisteIdSomBeholdes)
        }

        @Test
        fun `skal ikke kunne slette kjørelister som er innsendt av bruker`() {
            val fom = 5 januar 2026
            val tom = 18 januar 2026
            val (revurderingId, fagsakId) = opprettRammevedtakOgRevurdering(fom, tom, skalSendeInnKjørelisteForFørsteUka = true)

            val innsendtKjørelisteId = kjørelisteRepository.findByFagsakId(fagsakId).single().id

            kall.privatBil.apiRespons.slettManuellKjøreliste(revurderingId, innsendtKjørelisteId).expectProblemDetail(
                forventetStatus = HttpStatus.BAD_REQUEST,
                forventetDetail = "Kan ikke slette en kjøreliste som ikke er innsendt manuelt i denne behandlingen",
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
                kall.privatBil
                    .lagreManuellKjøreliste(
                        revurderingId,
                        LagreManuellKjørelisteRequest(
                            journalpostId = journalpostId(),
                            reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                            begrunnelse = null,
                            reisedager = lagKjørteDagerForUke(fom = fom, tom = tom, antallKjørteDager = 2),
                        ),
                    ).kjørelisteId

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
                kall.privatBil
                    .lagreManuellKjøreliste(
                        revurderingId,
                        LagreManuellKjørelisteRequest(
                            journalpostId = journalpostId(),
                            reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                            begrunnelse = null,
                            reisedager = lagKjørteDagerForUke(fom = 12 januar 2026, tom = tom, antallKjørteDager = 2),
                        ),
                    ).kjørelisteId

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

    @Nested
    inner class NullstillBehandling {
        @Test
        fun `skal beholde eksisterende kjørelister og slette nye ved nullstilling`() {
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
                kall.privatBil
                    .lagreManuellKjøreliste(
                        revurderingId,
                        LagreManuellKjørelisteRequest(
                            journalpostId = journalpostId(),
                            reiseId = kjørelisteOversikt.tilgjengeligeReiser.single().reiseId,
                            begrunnelse = null,
                            reisedager = lagKjørteDagerForUke(fom = 12 januar 2026, tom = tom, antallKjørteDager = 2),
                        ),
                    ).kjørelisteId

            kall.steg.ferdigstill(revurderingId, StegController.FerdigstillStegRequest(StegType.REGISTRER_KJØRELISTE))

            kall.behandling.nullstill(revurderingId)

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
