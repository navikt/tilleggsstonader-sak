package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate

data class LagreAktivitet(
    val behandlingId: BehandlingId,
    val type: AktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitetsdager: Int? = null,
    val prosent: Int? = null,
    val svarLønnet: SvarJaNei? = null,
    val svarHarUtgifter: SvarJaNei? = null,
    val begrunnelse: String? = null,
    val kildeId: String? = null,
)
