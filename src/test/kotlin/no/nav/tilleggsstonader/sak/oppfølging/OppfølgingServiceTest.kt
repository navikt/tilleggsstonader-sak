package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.OppfølgingRepositoryFake
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OppfølgingServiceTest {
    val repository = OppfølgingRepositoryFake()
    val service = OppfølgingService(repository)

    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Nested
    inner class IkkeAktiv {
        @Test
        fun `skal ikke kunne redigere en oppfølging som ikke lengre er aktiv`() {
            val oppfølging =
                repository.insert(
                    Oppfølging(
                        behandlingId = behandling.id,
                        data = OppfølgingData(emptyList()),
                        aktiv = false,
                    ),
                )

            assertThatThrownBy {
                service.kontroller(
                    KontrollerOppfølgingRequest(
                        oppfølging.id,
                        oppfølging.version,
                        KontrollertUtfall.HÅNDTERT,
                        "kommentar",
                    ),
                )
            }.hasMessageContaining("Kan ikke redigere en oppfølging som ikke lengre er aktiv")
        }
    }
}
