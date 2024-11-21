package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate
import java.time.LocalDate.now

fun lagreAktivitet(
    behandlingId: BehandlingId = BehandlingId.random(),
    type: AktivitetType = AktivitetType.TILTAK,
    fom: LocalDate = now(),
    tom: LocalDate = now(),
    aktivitetsdager: Int? = 5,
    svarLønnet: SvarJaNei? = null,
    begrunnelse: String? = null,
): LagreAktivitet = LagreAktivitet(
    type = type,
    behandlingId = behandlingId,
    fom = fom,
    tom = tom,
    faktaOgVurderinger = FaktaOgVurderingerAktivitetBarnetilsynDto(
        aktivitetsdager = aktivitetsdager,
        svarLønnet = svarLønnet,
    ),
    begrunnelse = begrunnelse,
)

val tiltak = LagreAktivitet(
    type = AktivitetType.TILTAK,
    behandlingId = BehandlingId.random(),
    fom = now(),
    tom = now(),
    faktaOgVurderinger = FaktaOgVurderingerAktivitetBarnetilsynDto(
        aktivitetsdager = 5,
        svarLønnet = null,
    ),
)

val ulønnetTiltak = lagreAktivitet(svarLønnet = SvarJaNei.NEI)

val utdanning = lagreAktivitet(type = AktivitetType.UTDANNING)

val ingenAktivitet = lagreAktivitet(
    type = AktivitetType.INGEN_AKTIVITET,
    begrunnelse = "Påkrevd begrunnelse",
)
