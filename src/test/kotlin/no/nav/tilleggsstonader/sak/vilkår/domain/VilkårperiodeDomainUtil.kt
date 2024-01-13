package no.nav.tilleggsstonader.sak.vilkår.domain

import java.time.LocalDate
import java.util.UUID

object VilkårperiodeDomainUtil {

    fun målgruppe(
        vilkårId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: MålgruppeType = MålgruppeType.AAP,
        detaljer: DetaljerMålgruppe = detaljerMålgruppe(),
    ) = Vilkårperiode(
        vilkårId = vilkårId,
        fom = fom,
        tom = tom,
        type = type,
        detaljer = detaljer,
    )

    fun detaljerMålgruppe() = DetaljerMålgruppe(
        medlemskap = SvarJaNei.JA_IMPLISITT,
    )

    fun aktivitet(
        vilkårId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: AktivitetType = AktivitetType.TILTAK,
        detaljer: DetaljerAktivitet = detaljerAktivitet(),
    ) = Vilkårperiode(
        vilkårId = vilkårId,
        fom = fom,
        tom = tom,
        type = type,
        detaljer = detaljer,
    )

    fun detaljerAktivitet() = DetaljerAktivitet(
        lønnet = SvarJaNei.NEI,
        mottarSykepenger = SvarJaNei.NEI,
    )
}
