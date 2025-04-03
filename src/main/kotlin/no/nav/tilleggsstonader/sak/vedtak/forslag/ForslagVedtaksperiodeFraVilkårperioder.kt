package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

/**
 * Forslag av vedtaksperioder i forhold til vilkårperioder.
 * Forholder seg ikke til ev. stønadsvilkår eks [VilkårType.PASS_BARN]
 */
data class ForslagVedtaksperiodeFraVilkårperioderGenerisk<MÅLGRUPPE>(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MÅLGRUPPE,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>

typealias ForslagVedtaksperiodeFraVilkårperioder = ForslagVedtaksperiodeFraVilkårperioderGenerisk<MålgruppeType>

typealias ForslagVedtaksperiodeFraVilkårperioderFaktiskMålgruppe = ForslagVedtaksperiodeFraVilkårperioderGenerisk<FaktiskMålgruppe>
