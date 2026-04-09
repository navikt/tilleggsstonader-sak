package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBoutgifterDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import java.time.LocalDate
import java.util.UUID

class VilkårperiodeRef internal constructor() {
    internal var resolvedId: UUID? = null

    val id: UUID
        get() = resolvedId ?: error("VilkårperiodeRef er ikke resolvt ennå. IDen er kun tilgjengelig etter at vilkårperioden er opprettet.")
}

@BehandlingTestdataDslMarker
class VilkårperiodeTestdataDsl {
    internal val opprettScope = OpprettVilkårperiodeDsl()
    internal val update = mutableListOf<(List<VilkårperiodeDto>, BehandlingId) -> Pair<UUID, LagreVilkårperiode>>()
    internal val delete = mutableListOf<(List<VilkårperiodeDto>) -> Pair<UUID, SlettVikårperiode>>()

    fun opprett(builder: OpprettVilkårperiodeDsl.() -> Unit) {
        opprettScope.apply(builder)
    }

    fun oppdater(builder: (List<VilkårperiodeDto>, BehandlingId) -> Pair<UUID, LagreVilkårperiode>) {
        update += builder
    }

    fun slett(builder: (List<VilkårperiodeDto>) -> Pair<UUID, SlettVikårperiode>) {
        delete += builder
    }
}

@BehandlingTestdataDslMarker
class OpprettVilkårperiodeDsl {
    private data class VilkårMedVilkårperiodeRef(
        val dto: (BehandlingId) -> LagreVilkårperiode,
        val ref: VilkårperiodeRef? = null,
    )

    private val vilkårForOpprettelse = mutableListOf<VilkårMedVilkårperiodeRef>()

    fun målgruppeAAP(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId ->
            lagreVilkårperiodeMålgruppe(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                målgruppeType = MålgruppeType.AAP,
            )
        }
    }

    fun målgruppeTiltakspenger(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId ->
            lagreVilkårperiodeMålgruppe(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                målgruppeType = MålgruppeType.TILTAKSPENGER,
            )
        }
    }

    fun målgruppeDagpenger(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId ->
            lagreVilkårperiodeMålgruppe(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                målgruppeType = MålgruppeType.DAGPENGER,
            )
        }
    }

    fun målgruppeOvergangsstønad(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId ->
            lagreVilkårperiodeMålgruppe(behandlingId = behandlingId, fom = fom, tom = tom, målgruppeType = MålgruppeType.OVERGANGSSTØNAD)
        }
    }

    fun aktivitetTiltakTilsynBarn(
        fom: LocalDate,
        tom: LocalDate,
        aktivitetsdager: Int,
    ): VilkårperiodeRef =
        addMedRef { behandlingId ->
            lagreVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                faktaOgSvar =
                    FaktaOgSvarAktivitetBarnetilsynDto(
                        svarLønnet = SvarJaNei.NEI,
                        aktivitetsdager = aktivitetsdager,
                    ),
            )
        }

    fun aktivitetTiltakBoutgifter(
        fom: LocalDate,
        tom: LocalDate,
    ): VilkårperiodeRef =
        addMedRef { behandlingId ->
            lagreVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                faktaOgSvar =
                    FaktaOgSvarAktivitetBoutgifterDto(
                        svarLønnet = SvarJaNei.NEI,
                    ),
            )
        }

    fun aktivitetTiltakLæremidler(
        fom: LocalDate,
        tom: LocalDate,
    ): VilkårperiodeRef =
        addMedRef { behandlingId ->
            lagreVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                faktaOgSvar =
                    FaktaOgSvarAktivitetLæremidlerDto(
                        prosent = 100,
                        studienivå = Studienivå.VIDEREGÅENDE,
                        svarHarUtgifter = SvarJaNei.JA,
                        svarHarRettTilUtstyrsstipend = SvarJaNei.NEI,
                    ),
            )
        }

    fun aktivitetTiltakTso(
        fom: LocalDate,
        tom: LocalDate,
    ): VilkårperiodeRef =
        addMedRef { behandlingId ->
            lagreVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                aktivitetType = AktivitetType.TILTAK,
                faktaOgSvar =
                    FaktaOgSvarAktivitetDagligReiseTsoDto(
                        svarLønnet = SvarJaNei.NEI,
                        svarHarUtgifter = SvarJaNei.JA,
                        aktivitetsdager = 3,
                    ),
            )
        }

    fun aktivitetTiltakTsr(
        fom: LocalDate,
        tom: LocalDate,
        typeAktivitet: TypeAktivitet,
        kildeId: String? = null,
    ): VilkårperiodeRef =
        addMedRef { behandlingId ->
            lagreVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                aktivitetType = AktivitetType.TILTAK,
                typeAktivitet = typeAktivitet,
                faktaOgSvar =
                    FaktaOgSvarAktivitetDagligReiseTsrDto(
                        svarHarUtgifter = SvarJaNei.JA,
                        aktivitetsdager = 3,
                    ),
                kildeId = kildeId,
            )
        }

    fun aktivitetUtdanningLæremidler(
        fom: LocalDate,
        tom: LocalDate,
    ): VilkårperiodeRef =
        addMedRef { behandlingId ->
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

    fun aktivitetUtdanningDagligReiseTso(
        fom: LocalDate,
        tom: LocalDate,
    ): VilkårperiodeRef =
        addMedRef { behandlingId ->
            lagreVilkårperiodeAktivitet(
                behandlingId = behandlingId,
                aktivitetType = AktivitetType.UTDANNING,
                fom = fom,
                tom = tom,
                faktaOgSvar =
                    FaktaOgSvarAktivitetDagligReiseTsoDto(
                        svarLønnet = SvarJaNei.NEI,
                        svarHarUtgifter = SvarJaNei.JA,
                        aktivitetsdager = 3,
                    ),
            )
        }

    fun add(lagreVilkår: (BehandlingId) -> LagreVilkårperiode) {
        vilkårForOpprettelse += VilkårMedVilkårperiodeRef(lagreVilkår)
    }

    private fun addMedRef(block: (BehandlingId) -> LagreVilkårperiode): VilkårperiodeRef {
        val ref = VilkårperiodeRef()
        vilkårForOpprettelse += VilkårMedVilkårperiodeRef(block, ref)
        return ref
    }

    fun build(behandlingId: BehandlingId): List<Pair<LagreVilkårperiode, VilkårperiodeRef?>> =
        vilkårForOpprettelse.map { it.dto(behandlingId) to it.ref }
}
