package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded

/**
 * Trenger vi noe mer enn data her? Kan den kanskje dekke alle tilfeller?
 * Eller om ma har vedtak, og beregningsgrunnlag som et eget?
 * Trenger man begrunnelse som eget felt?
 */
data class VedtakTilsynBarn(
    @Id
    val behandlingId: BehandlingId,
    val type: TypeVedtak,
    val vedtak: VedtaksdataTilsynBarn? = null,
    val beregningsresultat: BeregningsresultatTilsynBarn? = null,
    @Column("arsaker_avslag")
    val årsakerAvslag: ÅrsakAvslag.Wrapper? = null,
    val avslagBegrunnelse: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        when (type) {
            TypeVedtak.INNVILGELSE -> {
                require(beregningsresultat != null) { "Mangler beregningsresultat for type=$type" }
                require(vedtak != null) { "Mangler vedtak for type=$type" }
            }

            TypeVedtak.AVSLAG -> {
                require(årsakerAvslag != null && årsakerAvslag.årsaker.isNotEmpty()) { "Må velge minst en årsak for avslag" }
                require(avslagBegrunnelse != null) { "Avslag må begrunnes" }
            }

            TypeVedtak.OPPHØR -> TODO()
        }
    }
}

data class VedtaksdataTilsynBarn(
    val utgifter: Map<BarnId, List<Utgift>>,
)
