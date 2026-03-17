package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import java.time.LocalDate

data class DetaljertVedtaksperiodeDagligReise(
    val stønadstype: Stønadstype,
    val typeDagligReise: TypeDagligReise,
    val detaljertBeregningsperioder: List<DetaljertBeregningsperioder>?,
    val adresse: String?,
) : DetaljertVedtaksperiode

data class DetaljertBeregningsperioder(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
    val antallReisedager: Int,
    val antallReisedagerPerUke: Int,
) : Periode<LocalDate>,
    DetaljertVedtaksperiode {
    init {
        validatePeriode()
    }
}
