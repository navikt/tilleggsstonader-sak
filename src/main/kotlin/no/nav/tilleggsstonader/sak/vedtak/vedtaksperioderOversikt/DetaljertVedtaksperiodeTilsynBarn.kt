package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class DetaljertVedtaksperiodeTilsynBarn(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallBarn: Int,
    val totalMånedsUtgift: Int,
) : Periode<LocalDate>,
    Mergeable<LocalDate, DetaljertVedtaksperiodeTilsynBarn> {
    init {
        validatePeriode()
    }

    /**
     * Ettersom vedtaksperiode ikke overlapper er det tilstrekkelig å kun merge TOM
     */
    override fun merge(other: DetaljertVedtaksperiodeTilsynBarn): DetaljertVedtaksperiodeTilsynBarn = this.copy(tom = other.tom)

    fun erLikOgPåfølgesAv(other: DetaljertVedtaksperiodeTilsynBarn): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.antallBarn == other.antallBarn &&
                this.totalMånedsUtgift == other.totalMånedsUtgift
        val påfølgesAv = this.tom.plusDays(1) == other.fom
        return erLik && påfølgesAv
    }
}
