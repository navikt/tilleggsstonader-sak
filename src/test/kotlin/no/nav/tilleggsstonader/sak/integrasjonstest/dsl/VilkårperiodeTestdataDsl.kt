package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import java.time.LocalDate
import java.util.UUID

@BehandlingTestdataDslMarker
class VilkårperiodeTestdataDsl {
    internal val opprettScope = OpprettVilkårperiodeDsl()
    internal val update = mutableListOf<(BehandlingId) -> Pair<UUID, LagreVilkårperiode>>()
    internal val delete = mutableListOf<(BehandlingId) -> Pair<UUID, SlettVikårperiode>>()

    fun opprett(builder: OpprettVilkårperiodeDsl.() -> Unit) {
        opprettScope.apply(builder)
    }

    fun oppdater(builder: (BehandlingId) -> Pair<UUID, LagreVilkårperiode>) {
        update += builder
    }

    fun slett(builder: (BehandlingId) -> Pair<UUID, SlettVikårperiode>) {
        delete += builder
    }
}

@BehandlingTestdataDslMarker
class OpprettVilkårperiodeDsl {
    private val dtoer = mutableListOf<(BehandlingId) -> LagreVilkårperiode>()

    fun målgruppeAAP(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId ->
            lagreVilkårperiodeMålgruppe(behandlingId = behandlingId, fom = fom, tom = tom, målgruppeType = MålgruppeType.AAP)
        }
    }

    fun aktivitetTiltak(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId ->
            lagreVilkårperiodeAktivitet(behandlingId = behandlingId, fom = fom, tom = tom, aktivitetType = AktivitetType.TILTAK)
        }
    }

    fun aktivitetUtdanning(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId ->
            lagreVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                aktivitetType = AktivitetType.UTDANNING,
                fom = fom,
                tom = tom,
                faktaOgSvar =
                    FaktaOgSvarAktivitetLæremidlerDto(
                        prosent = 100,
                        studienivå = Studienivå.HØYERE_UTDANNING,
                        svarHarUtgifter = SvarJaNei.JA,
                        svarHarRettTilUtstyrsstipend = SvarJaNei.NEI,
                    ),
            )
        }
    }

    fun add(lagreVilkår: (BehandlingId) -> LagreVilkårperiode) {
        dtoer += lagreVilkår
    }

    fun build(behandlingId: BehandlingId) = dtoer.map { it.invoke(behandlingId) }
}
