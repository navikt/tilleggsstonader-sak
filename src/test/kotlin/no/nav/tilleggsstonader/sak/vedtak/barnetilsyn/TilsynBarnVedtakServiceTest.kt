package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class TilsynBarnVedtakServiceTest {

    private val tilsynBarnVedtakService = TilsynBarnVedtakService(mockk(), mockk(), mockk())

    @Test
    fun `skal mappe innvilget vedtak til dto`() {
        val vedtak = vedtak()

        val dto = tilsynBarnVedtakService.mapTilDto(vedtak) as InnvilgelseTilsynBarnDto

        assertThat(dto.utgifter).isEqualTo(vedtak.vedtak?.utgifter)
        assertThat(dto.beregningsresultat!!.perioder).isEqualTo(vedtak.beregningsresultat!!.perioder)
    }

    @Test
    fun `skal mappe avslått vedtak til dto`() {
        val vedtak = VedtakTilsynBarn(behandlingId = UUID.randomUUID(), type = TypeVedtak.AVSLAG, avslagBegrunnelse = "begrunnelse")

        val dto = tilsynBarnVedtakService.mapTilDto(vedtak) as AvslagTilsynBarnDto

        assertThat(dto.begrunnelse).isEqualTo(vedtak.avslagBegrunnelse)
        assertThat(dto.type).isEqualTo(vedtak.type)
    }

    private fun vedtak() = VedtakTilsynBarn(
        behandlingId = UUID.randomUUID(),
        type = TypeVedtak.INNVILGELSE,
        vedtak = VedtaksdataTilsynBarn(
            utgifter = mapOf(
                TilsynBarnTestUtil.barn(
                    UUID.randomUUID(),
                    Utgift(YearMonth.of(2023, 1), YearMonth.of(2023, 1), 100),
                ),
            ),
        ),
        beregningsresultat = VedtaksdataBeregningsresultat(
            perioder = listOf(
                Beregningsresultat(
                    dagsats = BigDecimal.TEN,
                    månedsbeløp = 1000,
                    grunnlag = Beregningsgrunnlag(
                        måned = YearMonth.now(),
                        makssats = 1000,
                        stønadsperioderGrunnlag = emptyList(),
                        utgifter = emptyList(),
                        utgifterTotal = 2000,
                        antallBarn = 1,
                    ),
                    beløpsperioder = listOf(
                        Beløpsperiode(dato = LocalDate.now(), beløp = 1000, målgruppe = MålgruppeType.AAP),
                        Beløpsperiode(dato = LocalDate.now().plusMonths(1), beløp = 2000, målgruppe = MålgruppeType.AAP),
                    ),
                ),
            ),
        ),
    )
}
