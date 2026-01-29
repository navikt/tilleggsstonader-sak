package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class DagligReisePrivatBilService {
    fun hentRammevedtaksPrivatBil(ident: IdentRequest): List<RammevedtakDto> {
        val beregningsresultat = hentBeregningsresultatMock()
        return mapBeregningsresultatTilRammevedtak(beregningsresultat)
    }

    private fun mapBeregningsresultatTilRammevedtak(beregningsresultat: BeregningsresultatPrivatBil): List<RammevedtakDto> =
        beregningsresultat.reiser.mapIndexed { index, reise ->
            RammevedtakDto(
                id = index.toString(),
                fom = reise.grunnlag.fom,
                tom = reise.grunnlag.tom,
                reisedagerPerUke = reise.grunnlag.reisedagerPerUke,
                aktivitetsadresse = "Ukjent adresse", // set if available
                aktivitetsnavn = "Ukjent aktivitet", // set if available
                uker =
                    reise.uker.mapIndexed { idx, uke ->
                        RammevedtakUkeDto(
                            fom = uke.grunnlag.fom,
                            tom = uke.grunnlag.tom,
                            ukeNummer = idx + 1,
                        )
                    },
            )
        }

    private fun hentBeregningsresultatMock(): BeregningsresultatPrivatBil =
        BeregningsresultatPrivatBil(
            reiser =
                listOf(
                    BeregningsresultatForReiseMedPrivatBil(
                        uker =
                            listOf(
                                BeregningsresultatForUke(
                                    grunnlag =
                                        BeregningsgrunnlagForUke(
                                            fom = LocalDate.of(2025, 1, 1),
                                            tom = LocalDate.of(2025, 1, 4),
                                            maksAntallDagerSomKanDekkes = 3,
                                            antallDagerInkludererHelg = true,
                                            vedtaksperioder = emptyList(),
                                            kilometersats = BigDecimal(4.03),
                                            dagsatsUtenParkering = BigDecimal(500),
                                        ),
                                    maksBeløpSomKanDekkesFørParkering = 100,
                                ),
                                BeregningsresultatForUke(
                                    grunnlag =
                                        BeregningsgrunnlagForUke(
                                            fom = LocalDate.of(2025, 1, 5),
                                            tom = LocalDate.of(2025, 1, 11),
                                            maksAntallDagerSomKanDekkes = 3,
                                            antallDagerInkludererHelg = true,
                                            vedtaksperioder = emptyList(),
                                            kilometersats = BigDecimal(4.03),
                                            dagsatsUtenParkering = BigDecimal(500),
                                        ),
                                    maksBeløpSomKanDekkesFørParkering = 100,
                                ),
                            ),
                        grunnlag =
                            BeregningsgrunnlagForReiseMedPrivatBil(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 11),
                                reisedagerPerUke = 3,
                                reiseavstandEnVei = BigDecimal(15),
                                ekstrakostnader =
                                    Ekstrakostnader(
                                        bompengerEnVei = 20,
                                        fergekostnadEnVei = 50,
                                    ),
                            ),
                    ),
                    BeregningsresultatForReiseMedPrivatBil(
                        uker =
                            listOf(
                                BeregningsresultatForUke(
                                    grunnlag =
                                        BeregningsgrunnlagForUke(
                                            fom = LocalDate.of(2025, 2, 2),
                                            tom = LocalDate.of(2025, 2, 8),
                                            maksAntallDagerSomKanDekkes = 4,
                                            antallDagerInkludererHelg = true,
                                            vedtaksperioder = emptyList(),
                                            kilometersats = BigDecimal(4.03),
                                            dagsatsUtenParkering = BigDecimal(500),
                                        ),
                                    maksBeløpSomKanDekkesFørParkering = 100,
                                ),
                                BeregningsresultatForUke(
                                    grunnlag =
                                        BeregningsgrunnlagForUke(
                                            fom = LocalDate.of(2025, 2, 9),
                                            tom = LocalDate.of(2025, 2, 13),
                                            maksAntallDagerSomKanDekkes = 4,
                                            antallDagerInkludererHelg = false,
                                            vedtaksperioder = emptyList(),
                                            kilometersats = BigDecimal(4.03),
                                            dagsatsUtenParkering = BigDecimal(500),
                                        ),
                                    maksBeløpSomKanDekkesFørParkering = 100,
                                ),
                            ),
                        grunnlag =
                            BeregningsgrunnlagForReiseMedPrivatBil(
                                fom = LocalDate.of(2025, 2, 2),
                                tom = LocalDate.of(2025, 2, 13),
                                reisedagerPerUke = 4,
                                reiseavstandEnVei = BigDecimal(15),
                                ekstrakostnader =
                                    Ekstrakostnader(
                                        bompengerEnVei = 200,
                                        fergekostnadEnVei = null,
                                    ),
                            ),
                    ),
                ),
        )
}
