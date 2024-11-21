package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate.now

val tiltak = LagreAktivitet(
    type = AktivitetType.TILTAK,
    behandlingId = BehandlingId.random(),
    fom = now(),
    tom = now(),
    aktivitetsdager = 5,
)

val ulønnetTiltak = tiltak.copy(
    svarLønnet = SvarJaNei.NEI,
)

val utdanning = tiltak.copy(
    type = AktivitetType.UTDANNING
)

val ingenAktivitet = tiltak.copy(
    type = AktivitetType.INGEN_AKTIVITET,
    begrunnelse = "Påkrevd begrunnelse"
)