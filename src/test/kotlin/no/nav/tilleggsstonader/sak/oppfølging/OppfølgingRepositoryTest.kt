package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
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
                utfall = KontrollertUtfall.HÅNDTERT,
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
            val revurdering = opprettRevurdering()

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

    @Nested
    inner class FinnSisteForBehandling {
        @Test
        fun `skal returnere null hvis det ikke finnes en for behandlingen`() {
            assertThat(oppfølgingRepository.finnSisteForFagsak(behandlingId)).isNull()
        }

        @Test
        fun `skal finne siste for behandling`() {
            oppfølgingRepository.insert(opprettOppfølging(SporbarUtils.now().minusDays(3)))
            val enDagSiden =
                oppfølgingRepository.insert(opprettOppfølging(SporbarUtils.now().minusDays(1)))
            oppfølgingRepository.insert(opprettOppfølging(SporbarUtils.now().minusDays(2)))

            val sisteForBehandling =
                oppfølgingRepository.finnSisteForFagsak(behandlingId)
            assertThat(sisteForBehandling?.id).isEqualTo(enDagSiden.id)
        }

        @Test
        fun `skal finne siste for fagsak`() {
            val revurdering = opprettRevurdering()
            val enDagSiden =
                oppfølgingRepository.insert(opprettOppfølging(SporbarUtils.now().minusDays(1), revurdering.id))
            oppfølgingRepository.insert(opprettOppfølging(SporbarUtils.now().minusDays(2)))

            val sisteForBehandling =
                oppfølgingRepository.finnSisteForFagsak(behandlingId)
            assertThat(sisteForBehandling?.id).isEqualTo(enDagSiden.id)
        }

        @Test
        fun `skal ikke finne treff på en annen fagsak`() {
            val annenBehandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    behandling = behandling(),
                    identer = fagsakpersoner(setOf("123")),
                )
            oppfølgingRepository.insert(opprettOppfølging(SporbarUtils.now().minusDays(1), annenBehandling.id))
            val toDagerSiden = oppfølgingRepository.insert(opprettOppfølging(SporbarUtils.now().minusDays(2)))

            val sisteForBehandling =
                oppfølgingRepository.finnSisteForFagsak(behandlingId)
            assertThat(sisteForBehandling?.id).isEqualTo(toDagerSiden.id)
        }

        fun opprettOppfølging(
            opprettet: LocalDateTime,
            behandlingId: BehandlingId = behandling.id,
        ) = Oppfølging(
            behandlingId = behandlingId,
            aktiv = false,
            data = data,
            opprettetTidspunkt = opprettet,
        )
    }

    private fun opprettRevurdering(): Behandling =
        testoppsettService.opprettRevurdering(
            revurderFra = LocalDate.now(),
            forrigeBehandling = behandling,
            fagsak = testoppsettService.hentFagsak(behandling.fagsakId),
        )
}
