package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table(name = "avklart_kjort_dag")
data class AvklartKjørtDag(
    @Id
    val id: UUID = UUID.randomUUID(),
    val dato: LocalDate,
    @Column("godkjent_gjennomfort_kjoring")
    val godkjentGjennomførtKjøring: GodkjentGjennomførtKjøring,
    val automatiskVurdering: UtfyltDagAutomatiskVurdering,
    val avvik: List<TypeAvvikDag>,
    val begrunnelse: String? = null,
    val parkeringsutgift: Int? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    @Column("avklart_kjort_dag_status")
    val avklartKjørtDagStatus: AvklartKjørtDagStatus,
) {
    fun markerSomSlettet(): AvklartKjørtDag = copy(avklartKjørtDagStatus = AvklartKjørtDagStatus.SLETTET)

    fun markerSomSlettetHvisEtter(fraDato: LocalDate): AvklartKjørtDag =
        if (dato > fraDato) {
            markerSomSlettet()
        } else {
            this
        }
}

fun Collection<AvklartKjørtDag>.markerSomSlettet(): Set<AvklartKjørtDag> = map { it.markerSomSlettet() }.toSet()

fun Collection<AvklartKjørtDag>.markerSomSlettet(fraDato: LocalDate): Set<AvklartKjørtDag> =
    map { dag -> dag.markerSomSlettetHvisEtter(fraDato) }.toSet()

enum class AvklartKjørtDagStatus {
    NY,
    ENDRET,
    UENDRET,
    SLETTET,
}

enum class GodkjentGjennomførtKjøring {
    JA,
    NEI,
    IKKE_VURDERT,
}

enum class UtfyltDagAutomatiskVurdering {
    OK,
    AVVIK,
}

enum class TypeAvvikDag {
    FOR_HØY_PARKERINGSUTGIFT,
    HELLIDAG_ELLER_HELG,
}
