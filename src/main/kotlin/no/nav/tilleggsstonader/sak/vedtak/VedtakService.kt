package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import org.springframework.data.annotation.Id
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.util.UUID

data class VedtakTilsynBarn(
    @Id
    val behandlingId: UUID,
    val perioder: List<String>,
    val beregningsresultat: List<String>,
)

@Repository
class VedtakTilsynBarnRepository {
    /**
     * Erstatt disse metodene når det er et faktiskt repository
     */
    fun findByIdOrNull(behandlingId: UUID): VedtakTilsynBarn? = null

    fun insert(vedtak: VedtakTilsynBarn): VedtakTilsynBarn = vedtak

    fun deleteById(behandlingId: UUID) {}
}

@Service
class VedtakService(
    private val vedtakTilsynBarnRepository: VedtakTilsynBarnRepository,
) {

    fun lagreVedtak(vedtak: VedtakTilsynBarn) {
        vedtakTilsynBarnRepository.insert(vedtak)
    }

    fun slettVedtak(saksbehandling: Saksbehandling) {
        when (saksbehandling.stønadstype) {
            Stønadstype.BARNETILSYN -> vedtakTilsynBarnRepository.deleteById(saksbehandling.id)
        }
    }
}
