package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

/**
 * Grupperer stønadsvilkår ("reiser") for reise oppstart/avslutning/hjemreise per aktivitet fra inngangsvilkår.
 * Inneholder alle aktiviteter for behandlingen, uavhengig av om det finnes reiser knyttet til dem ennå.
 */
data class AktivitetMedReiser(
    val aktivitetId: VilkårperiodeGlobalId,
    val aktivitetType: AktivitetType,
    val tiltaksvariant: TypeAktivitet?,
    val fom: LocalDate,
    val tom: LocalDate,
    val reiser: List<VilkårReiseOppstartAvslutningHjemreise>,
)
