package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

/**
 * Forslag av vedtaksperioder i forhold til vilkårperioder.
 * Forholder seg ikke til ev. stønadsvilkår eks [VilkårType.PASS_BARN]
 */
@Deprecated("Skal erstattes av finnVedtaksperiodeV2")
data class ForslagVedtaksperiodeFraVilkårperioder(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>

fun ForslagVedtaksperiodeFraVilkårperioder.tilVedtaksperiode() =
    Vedtaksperiode(
        id = UUID.randomUUID(),
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
