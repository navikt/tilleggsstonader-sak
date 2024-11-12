@file:Suppress("ktlint:standard:filename")

package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(DelvilkårMålgruppe::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(DelvilkårAktivitet::class, name = "AKTIVITET"),
)
sealed class DelvilkårVilkårperiode
