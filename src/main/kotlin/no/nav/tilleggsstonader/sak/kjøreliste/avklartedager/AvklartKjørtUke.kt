package no.nav.tilleggsstonader.sak.kjøreliste.avklartedager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table(name = "avklart_kjort_uke")
data class AvklartKjørtUke(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("behandling_id")
    val behandlingId: BehandlingId,
    val fom: LocalDate,
    val tom: LocalDate,
    val ukenummer: Int,
    val status: UkeStatus,
    val typeAvvik: TypeAvvikUke? = null,
    val behandletDato: LocalDate? = null,
    @MappedCollection(idColumn = "avklart_kjort_uke_id")
    val dager: List<AvklartKjørtDag>,
)

enum class UkeStatus {
    OK_AUTOMATISK, // brukes hvis automatisk godkjent
    OK_MANUELT, // brukes hvis saksbehandler godtar avvik
    AVVIK, // parkeringsutgifter/for mange dager etc. saksbehandler må ta stilling til uka
    IKKE_MOTTATT_KJØRELISTE,
}

enum class TypeAvvikUke {
    FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK,
}
