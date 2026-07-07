package no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("registrert_kjort_uke")
data class RegistrertKjørtUke(
    @Id val id: UUID = UUID.randomUUID(),
    @Column("behandling_id") val behandlingId: BehandlingId,
    val reiseId: ReiseId,
    val begrunnelse: String? = null,
    @MappedCollection(idColumn = "registrert_kjort_uke_id")
    val dager: Set<RegistrertKjørtDag> = emptySet(),
)
