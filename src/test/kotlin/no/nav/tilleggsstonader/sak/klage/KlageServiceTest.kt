package no.nav.tilleggsstonader.sak.klage

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansResultatDto
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsaker
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.klage.dto.OpprettKlageDto
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

internal class KlageServiceTest {
    private val fagsakService = mockk<FagsakService>()

    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    private val klageClient = mockk<KlageClient>()

    private val klageService =
        KlageService(
            fagsakService,
            arbeidsfordelingService,
            klageClient,
        )

    private val fagsakId = UUID.randomUUID()
    private val fagsakPersonId = UUID.randomUUID()
    private val eksternFagsakId = EksternFagsakId(1L, fagsakId)
    private val personIdent = "12345678910"
    private val arbeidsfordelingEnhetNr = "1"
    private val fagsak = fagsak(id = fagsakId, fagsakPersonId = fagsakPersonId, eksternId = eksternFagsakId)

    private val opprettKlageSlot = slot<OpprettKlagebehandlingRequest>()

    @BeforeEach
    internal fun setUp() {
        opprettKlageSlot.clear()
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { fagsakService.hentAktivIdent(fagsak.id) } returns personIdent
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet(
            arbeidsfordelingEnhetNr,
            "NAV arbeid og ytelser",
        )
        justRun { klageClient.opprettKlage(capture(opprettKlageSlot)) }
    }

    @Nested
    inner class OpprettKlage {
        @Test
        internal fun `skal mappe riktige verdier ved manuelt opprettet klage`() {
            val klageMottattTidspunkt = LocalDate.now()
            klageService.opprettKlage(fagsak.id, OpprettKlageDto(klageMottattTidspunkt))

            val request = opprettKlageSlot.captured

            assertThat(request.ident).isEqualTo("15")
            assertThat(request.stønadstype).isEqualTo(Stønadstype.BARNETILSYN)
            assertThat(request.eksternFagsakId).isEqualTo(eksternFagsakId.id.toString())
            assertThat(request.fagsystem).isEqualTo(Fagsystem.TILLEGGSSTONADER)
            assertThat(request.klageMottatt).isEqualTo(klageMottattTidspunkt)
            assertThat(request.behandlendeEnhet).isEqualTo(arbeidsfordelingEnhetNr)
            assertThat(request.klageGjelderTilbakekreving).isEqualTo(false)
        }
    }

    @Nested
    inner class Validering {
        @Test
        internal fun `skal ikke kunne opprette klage med krav mottatt frem i tid`() {
            val opprettKlageDto = OpprettKlageDto(mottattDato = LocalDate.now().plusDays(1))
            val feil = assertThrows<ApiFeil> { klageService.opprettKlage(UUID.randomUUID(), opprettKlageDto) }

            assertThat(feil.feil).contains("Kan ikke opprette klage med krav mottatt frem i tid for fagsak=")
        }

        @Test
        internal fun `skal ikke kunne opprette dersom enhetId ikke finnes`() {
            val opprettKlageDto = OpprettKlageDto(mottattDato = LocalDate.now())

            every { arbeidsfordelingService.hentNavEnhet(any()) } returns null

            val feil = assertThrows<ApiFeil> { klageService.opprettKlage(fagsak.id, opprettKlageDto) }

            assertThat(feil.feil).isEqualTo("Finner ikke behandlende enhet for personen")
        }
    }

    @Nested
    inner class Mapping {

        @Test
        internal fun `henter 0 behandlinger fra tilleggsstonader-klage med eksternFagsakId`() {
            val eksternFagsakIdSlot = slot<Set<Long>>()
            val fagsaker = fagsaker()

            every { fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(capture(eksternFagsakIdSlot)) } returns emptyMap()

            klageService.hentBehandlinger(fagsakPersonId)
            assertThat(eksternFagsakIdSlot.captured).containsExactlyInAnyOrder(eksternFagsakId.id)
        }

        @Test
        internal fun `skal mappe fagsakId til riktig stønadstype`() {
            val fagsaker = fagsaker()
            val klageBehandlingerDto = klageBehandlingerDto()

            every { fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(listOf(eksternFagsakId.id).toSet()) } returns klageBehandlingerDto

            val klager = klageService.hentBehandlinger(fagsakPersonId)

            assertThat(klager.barnetilsyn.single()).isEqualTo(klageBehandlingerDto[eksternFagsakId.id]!!.single())
        }

        @Test
        internal fun `skal returnere tomme lister dersom eksternFagsakId ikke eksisterer`() {
            every { fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId) } returns Fagsaker(null)

            val klager = klageService.hentBehandlinger(fagsakPersonId)

            assertThat(klager.barnetilsyn).isEmpty()
            verify(exactly = 0) { klageClient.hentKlagebehandlinger(any()) }
        }

        @Test
        internal fun `Hent klage - skal bruke vedtaksdato fra kabal hvis resultat IKKE_MEDHOLD og avsluttet i kabal`() {
            val fagsaker = fagsaker()

            val tidsPunktAvsluttetIKabal = LocalDateTime.of(2022, Month.OCTOBER, 1, 0, 0)
            val tidspunktAvsluttetIFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            val klagebehandlingAvsluttetKabal =
                klageBehandlingDto(
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat =
                    listOf(
                        KlageinstansResultatDto(
                            type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                            utfall = null,
                            mottattEllerAvsluttetTidspunkt = tidsPunktAvsluttetIKabal,
                            journalpostReferanser = listOf(),
                            årsakFeilregistrert = null,
                        ),
                    ),
                    vedtaksdato = tidspunktAvsluttetIFamilieKlage,
                )

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns mapOf(
                eksternFagsakId.id to listOf(
                    klagebehandlingAvsluttetKabal,
                ),
            )

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.barnetilsyn.first().vedtaksdato).isEqualTo(tidsPunktAvsluttetIKabal)
        }

        @Test
        internal fun `Hent klage - hvis resultat fra kabal ikke foreligger enda skal vedtaksdato være null behandlingsresultat er IKKE_MEDHOLD`() {
            val fagsaker = fagsaker()
            val tidspunktAvsluttetFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            val klagebehandlingIkkeAvsluttetKabal =
                klageBehandlingDto(
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat = emptyList(),
                    vedtaksdato = tidspunktAvsluttetFamilieKlage,
                )

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns
                mapOf(eksternFagsakId.id to listOf(klagebehandlingIkkeAvsluttetKabal))

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.barnetilsyn.first().vedtaksdato).isNull()
        }

        @Test
        internal fun `Hent klage - skal bruke vedtaksdato fra klageløsning dersom behandling ikke er oversendt kabal`() {
            val fagsaker = fagsaker()
            val tidspunktAvsluttetFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns
                mapOf(
                    eksternFagsakId.id to
                        listOf(
                            klageBehandlingDto(
                                resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
                                klageinstansResultat = emptyList(),
                                vedtaksdato = tidspunktAvsluttetFamilieKlage,
                            ),
                        ),
                )

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.barnetilsyn.first().vedtaksdato).isEqualTo(tidspunktAvsluttetFamilieKlage)
        }

        @Test
        internal fun `Hent klage - skal bruke vedtaksdato fra kabal dersom behandlingen er feilregistrert i kabal`() {
            val fagsaker = fagsaker()

            val tidsPunktAvsluttetIKabal = LocalDateTime.of(2022, Month.OCTOBER, 1, 0, 0)
            val tidspunktAvsluttetIFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            val årsakFeilregistrert = "Klage registrert på feil vedtak"
            val klagebehandlingAvsluttetKabal =
                klageBehandlingDto(
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat =
                    listOf(
                        KlageinstansResultatDto(
                            type = BehandlingEventType.BEHANDLING_FEILREGISTRERT,
                            utfall = null,
                            mottattEllerAvsluttetTidspunkt = tidsPunktAvsluttetIKabal,
                            journalpostReferanser = listOf(),
                            årsakFeilregistrert = årsakFeilregistrert,
                        ),
                    ),
                    vedtaksdato = tidspunktAvsluttetIFamilieKlage,
                )

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns
                mapOf(eksternFagsakId.id to listOf(klagebehandlingAvsluttetKabal))

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.barnetilsyn.first().vedtaksdato).isEqualTo(tidsPunktAvsluttetIKabal)
            assertThat(klager.barnetilsyn.first().klageinstansResultat.first().årsakFeilregistrert).isEqualTo(
                årsakFeilregistrert,
            )
        }

        private fun fagsaker() = Fagsaker(fagsak)

        private fun klageBehandlingDto(
            resultat: BehandlingResultat? = null,
            vedtaksdato: LocalDateTime? = null,
            klageinstansResultat: List<KlageinstansResultatDto> = emptyList(),
        ) = KlagebehandlingDto(
            id = UUID.randomUUID(),
            fagsakId = UUID.randomUUID(),
            status = BehandlingStatus.UTREDES,
            opprettet = LocalDateTime.now(),
            mottattDato = LocalDate.now().minusDays(1),
            resultat = resultat,
            årsak = null,
            vedtaksdato = vedtaksdato,
            klageinstansResultat = klageinstansResultat,
        )

        private fun klageBehandlingerDto() =
            mapOf(
                eksternFagsakId.id to listOf(klageBehandlingDto()),
            )
    }
}
