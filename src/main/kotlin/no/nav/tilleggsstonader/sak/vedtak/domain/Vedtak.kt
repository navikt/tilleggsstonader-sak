package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import no.nav.tilleggsstonader.sak.vedtak.girUtbetaling
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

typealias Vedtak = GeneriskVedtak<out Vedtaksdata>

@Table("vedtak")
data class GeneriskVedtak<T : Vedtaksdata>(
    @Id
    val behandlingId: BehandlingId,
    val data: T,
    val type: TypeVedtak = data.type.typeVedtak,
    val medUtbetaling: Boolean,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val gitVersjon: String?,
    val tidligsteEndring: LocalDate?,
    @Column("opphorsdato")
    val opphørsdato: LocalDate? = null,
) {
    constructor(
        behandlingId: BehandlingId,
        type: TypeVedtak,
        data: T,
        medUtbetaling: Boolean? = null,
        tidligsteEndring: LocalDate?,
        opphørsdato: LocalDate? = null,
    ) : this(
        behandlingId = behandlingId,
        data = data,
        type = type,
        medUtbetaling = medUtbetaling ?: type.girUtbetaling(),
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = tidligsteEndring,
        opphørsdato = opphørsdato
    )

    init {
        require(data.type.typeVedtak == type) { "$type på vedtak er ikke lik vedtak på data(${data.type.typeVedtak})" }
    }

    fun vedtaksperioderHvisFinnes(): List<Vedtaksperiode>? = takeIfType<HarVedtaksperioder>()?.data?.vedtaksperioder

}