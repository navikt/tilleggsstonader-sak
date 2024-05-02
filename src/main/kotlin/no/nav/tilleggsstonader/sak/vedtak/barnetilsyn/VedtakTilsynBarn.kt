package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

/**
 * Trenger vi noe mer enn data her? Kan den kanskje dekke alle tilfeller?
 * Eller om ma har vedtak, og beregningsgrunnlag som et eget?
 * Trenger man begrunnelse som eget felt?
 */
data class VedtakTilsynBarn(
    @Id
    val behandlingId: UUID,
    val type: TypeVedtak,
    val vedtak: VedtaksdataTilsynBarn? = null,
    val beregningsresultat: VedtaksdataBeregningsresultat? = null,
    val avslagBegrunnelse: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        when (type) {
            TypeVedtak.INNVILGET -> {
                require(beregningsresultat != null) { "Mangler beregningsresultat for type=$type" }
                require(vedtak != null) { "Mangler vedtak for type=$type" }
            }
            TypeVedtak.AVSLÅTT -> {
                require(avslagBegrunnelse != null) { "Avslag må begrunnes" }
            }
        }
    }
}

data class VedtaksdataTilsynBarn(
    val utgifter: Map<UUID, List<Utgift>>,
)

data class VedtaksdataBeregningsresultat(
    val perioder: List<Beregningsresultat>,
)
