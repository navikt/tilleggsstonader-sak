package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FagsakUtbetalingIdRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingIdRepository: FagsakUtbetalingIdRepository

    lateinit var behandling: Behandling

    @BeforeAll
    fun opprettFagsak() {
        behandling = testoppsettService.opprettBehandlingMedFagsak()
    }

    @Test
    fun `kan ikke opprette utbetalingid med samme fagsakid og typeAndel`() {
        fagsakUtbetalingIdRepository.insert(
            FagsakUtbetalingId(fagsakId = behandling.fagsakId, typeAndel = TypeAndel.DAGLIG_REISE_AAP, reiseId = null),
        )
        assertThatExceptionOfType(DuplicateKeyException::class.java).isThrownBy {
            fagsakUtbetalingIdRepository.insert(
                FagsakUtbetalingId(fagsakId = behandling.fagsakId, typeAndel = TypeAndel.DAGLIG_REISE_AAP, reiseId = null),
            )
        }
    }

    @Test
    fun `kan ikke opprette utbetalingid med samme fagsakid og typeAndel og reiseId`() {
        val reiseId = ReiseId.random()
        fagsakUtbetalingIdRepository.insert(
            FagsakUtbetalingId(fagsakId = behandling.fagsakId, typeAndel = TypeAndel.DAGLIG_REISE_AAP, reiseId = reiseId),
        )
        assertThatExceptionOfType(DuplicateKeyException::class.java).isThrownBy {
            fagsakUtbetalingIdRepository.insert(
                FagsakUtbetalingId(fagsakId = behandling.fagsakId, typeAndel = TypeAndel.DAGLIG_REISE_AAP, reiseId = reiseId),
            )
        }
    }

    @Test
    fun `kan opprette en utbetalingid med samme fagsakid og typeAndel med og uten reiseid`() {
        fagsakUtbetalingIdRepository.insert(
            FagsakUtbetalingId(fagsakId = behandling.fagsakId, typeAndel = TypeAndel.DAGLIG_REISE_AAP, reiseId = ReiseId.random()),
        )
        assertThatNoException().isThrownBy {
            fagsakUtbetalingIdRepository.insert(
                FagsakUtbetalingId(fagsakId = behandling.fagsakId, typeAndel = TypeAndel.DAGLIG_REISE_AAP, reiseId = null),
            )
        }
    }

    @AfterEach
    fun cleanUp() {
        fagsakUtbetalingIdRepository.deleteByFagsakId(behandling.fagsakId)
    }
}
