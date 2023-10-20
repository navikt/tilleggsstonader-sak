package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TilsynBarnVedtakRepository :
    VedtakRepository<VedtakTilsynBarn>,
    RepositoryInterface<VedtakTilsynBarn, UUID>,
    InsertUpdateRepository<VedtakTilsynBarn>

/**
 * Trenger vi noe mer enn data her? Kan den kanskje dekke alle tilfeller?
 * Eller om ma har vedtak, og beregningsgrunnlag som et eget?
 * Trenger man begrunnelse som eget felt?
 */
data class VedtakTilsynBarn(
    @Id
    val behandlingId: UUID,
    val type: TypeVedtak,
    val vedtak: VedtaksdataTilsynBarn,
    val beregningsresultat: VedtaksdataBeregningsresultat?,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class VedtaksdataTilsynBarn(
    val stønadsperioder: List<Stønadsperiode>,
    val utgifter: Map<UUID, List<Utgift>>,
)
data class VedtaksdataBeregningsresultat(
    val perioder: List<Beregningsresultat>,
)
