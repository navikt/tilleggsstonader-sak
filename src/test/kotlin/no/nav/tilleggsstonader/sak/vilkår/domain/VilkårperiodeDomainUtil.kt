package no.nav.tilleggsstonader.sak.vilkår.domain

import java.time.LocalDate
import java.util.UUID

object VilkårperiodeDomainUtil {

    fun målgruppe(
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: MålgruppeType = MålgruppeType.AAP,
        detaljer: DetaljerMålgruppe = detaljerMålgruppe(),
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        type = type,
        detaljer = detaljer,
        resultat = resultat,
    )

    fun detaljerMålgruppe() = DetaljerMålgruppe(
        medlemskap = SvarJaNei.JA_IMPLISITT,
    )

    fun aktivitet(
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: AktivitetType = AktivitetType.TILTAK,
        detaljer: DetaljerAktivitet = detaljerAktivitet(),
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        type = type,
        detaljer = detaljer,
        resultat = resultat,
    )

    fun detaljerAktivitet() = DetaljerAktivitet(
        lønnet = SvarJaNei.NEI,
        mottarSykepenger = SvarJaNei.NEI,
    )
}
