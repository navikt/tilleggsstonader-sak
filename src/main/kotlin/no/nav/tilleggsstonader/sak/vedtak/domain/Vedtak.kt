package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table

typealias Vedtak = GeneriskVedtak<out Vedtaksdata>

@Table("vedtak")
data class GeneriskVedtak<T : Vedtaksdata>(
    @Id
    val behandlingId: BehandlingId,
    val data: T,
    val type: TypeVedtak = data.type.typeVedtak,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        require(data.type.typeVedtak == type) { "$type på vedtak er ikke lik vedtak på data(${data.type.typeVedtak})" }
    }
}
