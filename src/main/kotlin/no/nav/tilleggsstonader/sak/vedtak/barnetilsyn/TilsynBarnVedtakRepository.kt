package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import org.springframework.data.annotation.Id
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TilsynBarnVedtakRepository {
    /**
     * Erstatt disse metodene når det er et faktiskt repository
     */
    fun findByIdOrNull(behandlingId: UUID): VedtakTilsynBarn? = null

    fun insert(vedtak: VedtakTilsynBarn): VedtakTilsynBarn = vedtak

    fun deleteById(behandlingId: UUID) {}
}

data class VedtakTilsynBarn(
    @Id
    val behandlingId: UUID,
    val perioder: List<String>,
    val beregningsresultat: List<String>,
)
