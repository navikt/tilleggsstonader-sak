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

class OppfølgningRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var oppfølgningRepository: OppfølgningRepository

    val behandling =
        behandling(
            resultat = BehandlingResultat.INNVILGET,
            status = BehandlingStatus.FERDIGSTILT,
            vedtakstidspunkt = LocalDateTime.now(),
        )
    val behandlingId = behandling.id
    val data =
        OppfølgingData(
            stønadstype = Stønadstype.BARNETILSYN,
            vedtakstidspunkt = LocalDateTime.now(),
            perioderTilKontroll = emptyList(),
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
    }

    @Test
    fun `skal kunne lagre og hente oppfølgning`() {
        val oppfølging = oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

        val fraDb = oppfølgningRepository.findByIdOrThrow(oppfølging.id)
        assertThat(fraDb).isEqualTo(oppfølging)
        assertThat(fraDb.version).isEqualTo(1)
        assertThat(fraDb.kontrollert).isNull()
    }

    @Test
    fun `skal oppdatere med kontrollert informasjon`() {
        val oppfølging = oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

        val kontrollert =
            Kontrollert(
                saksbehandler = "en saksbehandler",
                utfall = KontrollertUtfall.OK,
                kommentar = "en kommentar",
            )
        oppfølgningRepository.update(oppfølging.copy(kontrollert = kontrollert))

        val fraDb = oppfølgningRepository.findByIdOrThrow(oppfølging.id)
        assertThat(fraDb.version).isEqualTo(2)
        assertThat(fraDb.kontrollert).isEqualTo(kontrollert)
    }

    @Nested
    inner class MarkerAlleAktiveSomIkkeAktive {
        @Test
        fun `skal oppdatere alle aktive til ikke aktive og oppdatere version`() {
            val oppfølging = oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

            oppfølgningRepository.markerAlleAktiveSomIkkeAktive()

            val fraDb = oppfølgningRepository.findByIdOrThrow(oppfølging.id)
            assertThat(fraDb.version).isEqualTo(2)
            assertThat(fraDb.aktiv).isFalse()
        }
    }

    @Nested
    inner class FinnAktiveMedDetaljer {
        @Test
        fun `skal finne alle aktive`() {
            oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))

            val aktive = oppfølgningRepository.finnAktiveMedDetaljer()

            assertThat(aktive).hasSize(1)
            assertThat(aktive.single().harNyereBehandling).isFalse()
        }

        @Test
        fun `skal finne alle aktive med informasjon om at det finnes en ny behandling`() {
            val revurdering =
                testoppsettService.opprettRevurdering(
                    revurderFra = LocalDate.now(),
                    forrigeBehandling = behandling,
                    fagsak = testoppsettService.hentFagsak(behandling.fagsakId),
                )

            oppfølgningRepository.insert(Oppfølging(behandlingId = revurdering.forrigeBehandlingId!!, data = data))

            val aktive = oppfølgningRepository.finnAktiveMedDetaljer()
            assertThat(aktive).hasSize(1)
            assertThat(aktive.single().harNyereBehandling).isTrue()
        }

        @Test
        fun `skal ikke finne noen hvis det kun finnes ikke aktive`() {
            oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data, aktiv = false))
            assertThat(oppfølgningRepository.finnAktiveMedDetaljer()).isEmpty()
        }
    }
}
