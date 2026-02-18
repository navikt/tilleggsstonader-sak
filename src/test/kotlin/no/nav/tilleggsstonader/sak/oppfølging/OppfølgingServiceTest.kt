package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.OppfølgingRepositoryFake
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OppfølgingServiceTest {
    val repository = OppfølgingRepositoryFake()
    val service = OppfølgingService(repository)

    val behandlingTilsynBarn = behandling()
    val behandlingDagligReiseTSR =
        behandling(
            fagsak =
                fagsak(
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                ),
        )

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
                        behandlingId = behandlingTilsynBarn.id,
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

    @Test
    fun `hent oppgølginger henter bare oppgaver for NAY`() {
        val oppfølgingTsr =
            repository.insert(
                Oppfølging(
                    behandlingId = behandlingDagligReiseTSR.id,
                    data = OppfølgingData(emptyList()),
                    aktiv = false,
                ),
            )
        val oppfølgingNay =
            repository.insert(
                Oppfølging(
                    behandlingId = behandlingTilsynBarn.id,
                    data = OppfølgingData(emptyList()),
                    aktiv = false,
                ),
            )

        assertThat(
            service
                .hentAktiveOppfølginger(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)
                .single()
                .behandlingsdetaljer
                .stønadstype
                .behandlendeEnhet(),
        ).isEqualTo(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)
    }
}
