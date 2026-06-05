package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.util.validerUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
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
    val aktivitetType: AktivitetType,
    val tiltaksvariant: TypeAktivitet?,
    val grunnlag: RammeForReiseMedPrivatBilBeregningsgrunnlag,
) {
    fun finnDelperiodeForPeriode(periode: Periode<LocalDate>) = grunnlag.delperioder.single { it.inneholder(periode) }

    fun avkortEtterDato(maksTom: LocalDate): RammeForReiseMedPrivatBil? {
        val avkortetGrunnlag = grunnlag.avkortEtterDato(maksTom) ?: return null

        return copy(
            grunnlag = avkortetGrunnlag,
        )
    }
}

data class RammeForReiseMedPrivatBilBeregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val delperioder: List<RammeForReiseMedPrivatBilDelperiode>,
    val reiseavstandEnVei: BigDecimal,
    val vedtaksperioder: List<Vedtaksperiode>,
) : Periode<LocalDate>,
    KopierPeriode<RammeForReiseMedPrivatBilBeregningsgrunnlag> {
    init {
        validerUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode(
            overordnetPeriode = this,
            delperioder = delperioder,
        )
    }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): RammeForReiseMedPrivatBilBeregningsgrunnlag = this.copy(fom = fom, tom = tom)

    fun vedtaksperiodeForPeriode(periode: Periode<LocalDate>) = vedtaksperioder.single { it.inneholder(periode) }

    fun avkortEtterDato(maksTom: LocalDate): RammeForReiseMedPrivatBilBeregningsgrunnlag? {
        if (maksTom < fom) return null
        if (tom <= maksTom) return this

        val avkortedeDelperioder = delperioder.mapNotNull { it.avkortEtterDato(maksTom) }
        return copy(
            tom = maksTom,
            delperioder = avkortedeDelperioder,
            vedtaksperioder = vedtaksperioder.avkortFraOgMed(maksTom),
        )
    }
}

data class RammeForReiseMedPrivatBilDelperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val ekstrakostnader: RammeForReiseMedPrivatEkstrakostnader,
    val reisedagerPerUke: Int,
    val satser: List<RammeForReiseMedPrivatBilSatsForDelperiode>,
) : Periode<LocalDate>,
    KopierPeriode<RammeForReiseMedPrivatBilDelperiode> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): RammeForReiseMedPrivatBilDelperiode = this.copy(fom = fom, tom = tom)

    fun finnSatsForDato(dato: LocalDate): RammeForReiseMedPrivatBilSatsForDelperiode = satser.single { it.inneholder(dato) }

    fun avkortEtterDato(maksTom: LocalDate): RammeForReiseMedPrivatBilDelperiode? {
        val avkortetPeriode = this.avkortFraOgMed(maksTom) ?: return null

        return avkortetPeriode.copy(
            satser = satser.avkortFraOgMed(maksTom),
        )
    }
}

data class RammeForReiseMedPrivatBilSatsForDelperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal, // hva brukeren kan få dekt per dag. Inkluderer bompenger og ferge, men ikke parkering.
    val satsBekreftetVedVedtakstidspunkt: Boolean,
) : Periode<LocalDate>,
    KopierPeriode<RammeForReiseMedPrivatBilSatsForDelperiode> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): RammeForReiseMedPrivatBilSatsForDelperiode = this.copy(fom = fom, tom = tom)
}

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
