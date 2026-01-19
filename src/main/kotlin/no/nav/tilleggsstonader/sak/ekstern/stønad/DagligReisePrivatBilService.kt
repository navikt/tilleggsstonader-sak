package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReisePrivatBilService(
    private val fagsakService: FagsakService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
) {
    fun hentRammevedtaksPrivatBil(ident: IdentStønadstype): List<RammevedtakDto> = hentRammevedtakMock()

    private fun hentRammevedtakMock(): List<RammevedtakDto> =
        listOf(
            RammevedtakDto(
                id = "1",
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 6),
                reisedagerPerUke = 3,
                aktivitetsadresse = "Tiurveien 34, 0356 Oslo",
                aktivitetsnavn = "Arbeidstrening",
                uker =
                    listOf(
                        RammevedtakUkeDto(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 1, 5),
                            ukeNummer = 1,
                        ),
                        RammevedtakUkeDto(
                            fom = LocalDate.of(2025, 1, 6),
                            tom = LocalDate.of(2025, 1, 12),
                            ukeNummer = 2,
                        ),
                        RammevedtakUkeDto(
                            fom = LocalDate.of(2025, 1, 13),
                            tom = LocalDate.of(2025, 1, 19),
                            ukeNummer = 3,
                        ),
                        RammevedtakUkeDto(
                            fom = LocalDate.of(2025, 1, 20),
                            tom = LocalDate.of(2025, 1, 26),
                            ukeNummer = 4,
                        ),
                        RammevedtakUkeDto(
                            fom = LocalDate.of(2025, 1, 27),
                            tom = LocalDate.of(2025, 2, 2),
                            ukeNummer = 5,
                        ),
                        RammevedtakUkeDto(
                            fom = LocalDate.of(2025, 2, 3),
                            tom = LocalDate.of(2025, 2, 6),
                            ukeNummer = 6,
                        ),
                    ),
            ),
            RammevedtakDto(
                id = "2",
                fom = LocalDate.of(2025, 2, 10),
                tom = LocalDate.of(2025, 2, 16),
                reisedagerPerUke = 3,
                aktivitetsadresse = "Drammensveien 1, 0356 Oslo",
                aktivitetsnavn = "Tiltak",
                uker =
                    listOf(
                        RammevedtakUkeDto(
                            fom = LocalDate.of(2025, 2, 10),
                            tom = LocalDate.of(2025, 2, 16),
                            ukeNummer = 7,
                        ),
                    ),
            ),
        )
}
