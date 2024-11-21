package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.målgruppe

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import java.time.LocalDate

data class LagreMålgruppe(
    val behandlingId: BehandlingId,
    val type: MålgruppeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val prosent: Int? = null,
    val svarMedlemskap: SvarJaNei? = null,
    val svarUtgifterDekketAvAnnetRegelverk: SvarJaNei? = null,
    val begrunnelse: String? = null,
    val kildeId: String? = null,
)
