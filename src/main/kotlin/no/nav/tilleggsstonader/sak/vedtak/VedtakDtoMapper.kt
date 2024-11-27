package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import java.time.LocalDate

object VedtakDtoMapper {

    fun toDto(vedtak: Vedtak, revurderFra: LocalDate?): VedtakResponse {
        return when (vedtak.data) {
            is InnvilgelseTilsynBarn -> InnvilgelseTilsynBarnResponse(
                beregningsresultat = vedtak.data.beregningsresultat.tilDto(revurderFra = revurderFra),
            )

            is OpphørTilsynBarn -> OpphørTilsynBarnDto(
                årsakerOpphør = vedtak.data.årsaker,
                begrunnelse = vedtak.data.begrunnelse,
            )

            is AvslagTilsynBarn -> AvslagTilsynBarnDto(
                årsakerAvslag = vedtak.data.årsaker,
                begrunnelse = vedtak.data.begrunnelse,
            )
        }
    }
}
