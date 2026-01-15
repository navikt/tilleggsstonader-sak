package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import java.util.UUID

/**
 * Builder-style DSL used by `gjennomførBehandlingsløp(...){ ... }` in integration tests.
 *
 * Supports create/update/delete for:
 *  - aktivitet (vilkårperiode)
 *  - målgruppe (vilkårperiode)
 *  - vilkår (stønadsvilkår)
 */
class BehandlingTestdataDsl internal constructor() {
    internal val aktivitet: VilkårperiodeTestdataDsl = VilkårperiodeTestdataDsl()
    internal val målgruppe: VilkårperiodeTestdataDsl = VilkårperiodeTestdataDsl()
    internal val vilkår: StønadsvilkårTestdataDsl = StønadsvilkårTestdataDsl()

    fun aktivitet(block: VilkårperiodeTestdataDsl.() -> Unit) {
        aktivitet.apply(block)
    }

    fun målgruppe(block: VilkårperiodeTestdataDsl.() -> Unit) {
        målgruppe.apply(block)
    }

    fun vilkår(block: StønadsvilkårTestdataDsl.() -> Unit) {
        vilkår.apply(block)
    }

    companion object {
        fun build(block: BehandlingTestdataDsl.() -> Unit): BehandlingTestdataDsl = BehandlingTestdataDsl().apply(block)
    }
}

class VilkårperiodeTestdataDsl {
    internal val create = mutableListOf<(BehandlingId) -> LagreVilkårperiode>()
    internal val update = mutableListOf<(BehandlingId) -> Pair<UUID, LagreVilkårperiode>>()
    internal val delete = mutableListOf<(BehandlingId) -> Pair<UUID, SlettVikårperiode>>()

    fun opprett(builder: (BehandlingId) -> LagreVilkårperiode) {
        create += builder
    }

    fun oppdater(builder: (BehandlingId) -> Pair<UUID, LagreVilkårperiode>) {
        update += builder
    }

    fun slett(builder: (BehandlingId) -> Pair<UUID, SlettVikårperiode>) {
        delete += builder
    }
}

class StønadsvilkårTestdataDsl {
    internal val create = mutableListOf<LagreVilkår>()
    internal val update = mutableListOf<(BehandlingId) -> SvarPåVilkårDto>()
    internal val delete = mutableListOf<(BehandlingId) -> SlettVilkårRequest>()

    fun opprettVilkår(builder: () -> LagreVilkår) {
        create += builder()
    }

    fun opprett(vararg vilkår: LagreVilkår) {
        create += vilkår
    }

    fun oppdater(block: (behandlingId: BehandlingId) -> SvarPåVilkårDto) {
        update += block
    }

    fun slett(block: (behandlingId: BehandlingId) -> SlettVilkårRequest) {
        delete += block
    }
}
