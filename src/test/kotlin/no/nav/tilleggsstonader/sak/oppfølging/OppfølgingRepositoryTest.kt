package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class OppfølgingRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var oppfølgingRepository: OppfølgingRepository

    val behandling =
        behandling(
            resultat = BehandlingResultat.INNVILGET,
            status = BehandlingStatus.FERDIGSTILT,
            vedtakstidspunkt = LocalDateTime.now(),
        )
    val behandlingId = behandling.id
    val data = OppfølgingData(perioderTilKontroll = emptyList())

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
    }

    @Test
    fun `skal kunne lagre og hente oppfølgning`() {
        val oppfølging = oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

        val fraDb = oppfølgingRepository.findByIdOrThrow(oppfølging.id)
        assertThat(fraDb).isEqualTo(oppfølging)
        assertThat(fraDb.version).isEqualTo(1)
        assertThat(fraDb.kontrollert).isNull()
    }

    @Test
    fun `skal oppdatere med kontrollert informasjon`() {
        val oppfølging = oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

        val kontrollert =
            Kontrollert(
                saksbehandler = "en saksbehandler",
                utfall = KontrollertUtfall.OK,
                kommentar = "en kommentar",
            )
        oppfølgingRepository.update(oppfølging.copy(kontrollert = kontrollert))

        val fraDb = oppfølgingRepository.findByIdOrThrow(oppfølging.id)
        assertThat(fraDb.version).isEqualTo(2)
        assertThat(fraDb.kontrollert).isEqualTo(kontrollert)
    }

    @Nested
    inner class MarkerAlleAktiveSomIkkeAktive {
        @Test
        fun `skal oppdatere alle aktive til ikke aktive og oppdatere version`() {
            val oppfølging = oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

            oppfølgingRepository.markerAlleAktiveSomIkkeAktive()

            val fraDb = oppfølgingRepository.findByIdOrThrow(oppfølging.id)
            assertThat(fraDb.version).isEqualTo(2)
            assertThat(fraDb.aktiv).isFalse()
        }
    }

    @Nested
    inner class FinnAktiveMedDetaljer {
        @Test
        fun `skal finne alle aktive`() {
            oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

            val aktive = oppfølgingRepository.finnAktiveMedDetaljer()

            assertThat(aktive).hasSize(1)
            assertThat(aktive.single().behandlingsdetaljer.harNyereBehandling).isFalse()
        }

        @Test
        fun `skal finne alle aktive med informasjon om at det finnes en ny behandling`() {
            val revurdering =
                testoppsettService.opprettRevurdering(
                    revurderFra = LocalDate.now(),
                    forrigeBehandling = behandling,
                    fagsak = testoppsettService.hentFagsak(behandling.fagsakId),
                )

            oppfølgingRepository.insert(Oppfølging(behandlingId = revurdering.forrigeBehandlingId!!, data = data))

            val aktive = oppfølgingRepository.finnAktiveMedDetaljer()
            assertThat(aktive).hasSize(1)
            assertThat(aktive.single().behandlingsdetaljer.harNyereBehandling).isTrue()
        }

        @Test
        fun `skal ikke finne noen hvis det kun finnes ikke aktive`() {
            oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data, aktiv = false))
            assertThat(oppfølgingRepository.finnAktiveMedDetaljer()).isEmpty()
        }

        @Test
        fun `skal finne aktiv for behandling`() {
            oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

            val aktiv = oppfølgingRepository.finnAktivMedDetaljer(behandlingId)

            assertThat(aktiv.behandlingsdetaljer.stønadstype).isEqualTo(Stønadstype.BARNETILSYN)
        }
    }
}
