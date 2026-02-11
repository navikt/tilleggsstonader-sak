package no.nav.tilleggsstonader.sak.kjøreliste.avklartedager

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table(name = "avklart_kjort_dag")
data class AvklartKjørtDag(
    @Id
    val id: UUID = UUID.randomUUID(),
    val dato: LocalDate,
    @Column("godkjent_gjennomfort_kjoring")
    val godkjentGjennomfortKjøring: Boolean,
    val automatiskVurdering: UtfyltDagAutomatiskVurdering,
    val avvik: List<TypeAvvikDag>,
    val begrunnelse: String? = null,
    val parkeringsutgift: Int? = null,
)

enum class UtfyltDagAutomatiskVurdering {
    OK,
    AVVIK,
}

enum class TypeAvvikDag {
    FOR_HØY_PARKERINGSUTGIFT,
    HELLIDAG_ELLER_HELG,
}
