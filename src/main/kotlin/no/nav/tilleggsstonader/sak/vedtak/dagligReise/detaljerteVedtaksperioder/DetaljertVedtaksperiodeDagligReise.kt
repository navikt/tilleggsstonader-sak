package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import java.time.LocalDate

data class DetaljertBeregningsperioderDagligReise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
    val antallReisedager: Int,
    val antallReisedagerPerUke: Int,
    val stønadstype: Stønadstype,
    val typeDagligReise: TypeDagligReise,
) : Periode<LocalDate>,
    DetaljertVedtaksperiode,
    Mergeable<LocalDate, DetaljertBeregningsperioderDagligReise> {
    init {
        validatePeriode()
    }

    override fun merge(other: DetaljertBeregningsperioderDagligReise): DetaljertBeregningsperioderDagligReise = this.copy(tom = other.tom)
}
