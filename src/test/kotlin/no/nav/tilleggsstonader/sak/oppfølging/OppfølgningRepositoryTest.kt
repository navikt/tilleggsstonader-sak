package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OppfølgningRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var oppfølgningRepository: OppfølgningRepository

    val behandlingId = BehandlingId.random()
    val data = OppfølgingData(behandlingId = behandlingId, perioderTilKontroll = emptyList())

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

        val kontrollert = Kontrollert(saksbehandler = "en saksbehandler", kommentar = "en kommentar")
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
    inner class FindByAktivIsTrue {
        @Test
        fun `skal finne alle aktive`() {
            oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))
            assertThat(oppfølgningRepository.findByAktivIsTrue()).hasSize(1)
        }

        @Test
        fun `skal ikke finne noen hvis det kun finnes ikke aktive`() {
            oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data, aktiv = false))
            assertThat(oppfølgningRepository.findByAktivIsTrue()).isEmpty()
        }
    }
}
