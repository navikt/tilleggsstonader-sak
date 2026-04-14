package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.allePerioderErSammenhengende
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

data class RammevedtakPrivatBil(
    val reiser: List<RammeForReiseMedPrivatBil>,
) {
    fun hentRammevedtakForReise(reiseId: ReiseId): RammeForReiseMedPrivatBil = reiser.single { it.reiseId == reiseId }
}

data class RammeForReiseMedPrivatBil(
    val reiseId: ReiseId,
    val aktivitetsadresse: String?,
    val grunnlag: RammeForReiseMedPrivatBilBeregningsgrunnlag,
) {
    fun finnDelperiodeForPeriode(periode: Periode<LocalDate>) = grunnlag.delperioder.single { it.inneholder(periode) }
}

data class RammeForReiseMedPrivatBilBeregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val delperioder: List<RammeForReiseMedPrivatBilDelperiode>,
    val reiseavstandEnVei: BigDecimal,
    val vedtaksperioder: List<Vedtaksperiode>,
) : Periode<LocalDate> {
    init {
        brukerfeilHvis(fom != delperioder.minOf { it.fom }) {
            "Rammevedtaket sin fom ${fom.norskFormat()} er ikke lik den tidligste delperioden sin fom ${delperioder.minOf {
                it.fom
                    .norskFormat()
            }}"
        }
        brukerfeilHvis(tom != delperioder.maxOf { it.tom }) {
            "Rammevedtaket sin tom ${tom.norskFormat()} er ikke lik den tidligste delperioden sin tom ${delperioder.maxOf {
                it.tom
                    .norskFormat()
            }}"
        }
        brukerfeilHvisIkke(delperioder.allePerioderErSammenhengende()) {
            "Alle delperioder må være sammenhengende: " +
                delperioder.joinToString(", ") { "${it.fom.norskFormat()} - ${it.tom.norskFormat()}" }
        }
    }

    fun vedtaksperiodeForPeriode(periode: Periode<LocalDate>) = vedtaksperioder.single { it.inneholder(periode) }
}

data class RammeForReiseMedPrivatBilDelperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val ekstrakostnader: RammeForReiseMedPrivatEkstrakostnader,
    val reisedagerPerUke: Int,
    val satser: List<RammeForReiseMedPrivatBilSatsForDelperiode>,
) : Periode<LocalDate> {
    fun finnSatsForDato(dato: LocalDate): RammeForReiseMedPrivatBilSatsForDelperiode = satser.single { it.inneholder(dato) }
}

data class RammeForReiseMedPrivatBilSatsForDelperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal, // hva brukeren kan få dekt per dag. Inkluderer bompenger og ferge, men ikke parkering.
    val satsBekreftetVedVedtakstidspunkt: Boolean,
) : Periode<LocalDate>

data class RammeForReiseMedPrivatEkstrakostnader(
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
) {
    fun beregnTotalEkstrakostnadForEnDag(): BigDecimal {
        val bompengerEnDag = bompengerPerDag ?: 0
        val fergekostnadEnDag = fergekostnadPerDag ?: 0

        val sum = bompengerEnDag + fergekostnadEnDag

        return sum.toBigDecimal()
    }
}
