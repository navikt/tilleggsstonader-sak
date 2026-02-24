package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

// Legger på Include.NON_NULL for å unngå at vi serialiserer "typeAktivitet": null" i JSON for TSO som er i produksjon
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VedtaksperiodeGrunnlag(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val typeAktivitet: TypeAktivitet?,
    val antallReisedagerIVedtaksperioden: Int,
) : Periode<LocalDate> {
    constructor(vedtaksperiode: Vedtaksperiode, antallReisedager: Int) : this(
        id = vedtaksperiode.id,
        fom = vedtaksperiode.fom,
        tom = vedtaksperiode.tom,
        målgruppe = vedtaksperiode.målgruppe,
        aktivitet = vedtaksperiode.aktivitet,
        typeAktivitet = vedtaksperiode.typeAktivitet,
        antallReisedagerIVedtaksperioden = antallReisedager,
    )
}
