package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

object VedtaksperiodeBeregningTestUtil {
    fun vedtaksperiodeBeregning(
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = VedtaksperiodeBeregning(
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}
