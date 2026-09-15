package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
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
    @Column("kjoreliste_id")
    val kjørelisteId: KjørelisteId,
    val reiseId: ReiseId,
    override val fom: LocalDate,
    override val tom: LocalDate,
    @Column("uke")
    val uke: UkeIÅr,
    val status: UkeStatus,
    @MappedCollection(idColumn = "avklart_kjort_uke_id")
    val avvik: Set<AvklartKjørtUkeAvvik> = emptySet(),
    @MappedCollection(idColumn = "avklart_kjort_uke_id")
    val dager: Set<AvklartKjørtDag>,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    @Column("avklart_kjort_uke_status")
    val avklartKjørtUkeStatus: AvklartKjørtUkeStatus,
) : Periode<LocalDate> {
    init {
        require(dager.all { inneholder(it.dato) }) { "Alle dager må være innenfor perioden til uken" }
        require(fom.tilUkeIÅr() == tom.tilUkeIÅr()) { "Fom og tom må være i samme uke" }
        require(fom.tilUkeIÅr() == uke) { "Ukenummer $uke stemmer ikke med perioden" }
    }

    fun kopierTilNyBehandling(nyBehandlingId: BehandlingId): AvklartKjørtUke =
        copy(
            id = UUID.randomUUID(),
            behandlingId = nyBehandlingId,
            avvik = avvik.map { it.copy(id = UUID.randomUUID()) }.toSet(),
            dager =
                dager
                    .filter { it.avklartKjørtDagStatus != AvklartKjørtDagStatus.SLETTET }
                    .map { it.copy(id = UUID.randomUUID(), avklartKjørtDagStatus = AvklartKjørtDagStatus.UENDRET) }
                    .toSet(),
            avklartKjørtUkeStatus = AvklartKjørtUkeStatus.UENDRET,
        )
}

/**
 * Wrapper-entitet for å representere [TypeAvvikUke] som en liste på [AvklartKjørtUke].
 *
 * Spring Data JDBC klarer ikke (per Spring Data 4.1) å konvertere et VARCHAR[]-array til en
 * List<Enum> når property'et ligger direkte på aggregatroten - i motsetning til når det ligger
 * på en nested @MappedCollection-entitet (som f.eks. [AvklartKjørtDag.avvik]), der default-
 * konverteringen fungerer korrekt. Vi bruker derfor samme, velprøvde mønster her (egen
 * child-tabell) i stedet for et array-felt direkte på [AvklartKjørtUke].
 */
@Table(name = "avklart_kjort_uke_avvik")
data class AvklartKjørtUkeAvvik(
    @Id
    val id: UUID = UUID.randomUUID(),
    val typeAvvik: TypeAvvikUke,
)

/**
 * Bekvemmelighetsfunksjon for å lese avvikene på uken som en enkel liste av [TypeAvvikUke],
 * uten å måtte forholde seg til wrapper-entiteten [AvklartKjørtUkeAvvik] i forretningslogikken.
 */
val AvklartKjørtUke.typeAvvik: List<TypeAvvikUke>
    get() = avvik.map { it.typeAvvik }

fun List<TypeAvvikUke>.tilAvklartKjørtUkeAvvik(): Set<AvklartKjørtUkeAvvik> = this.map { AvklartKjørtUkeAvvik(typeAvvik = it) }.toSet()

enum class UkeStatus {
    OK_AUTOMATISK, // brukes hvis automatisk godkjent
    OK_MANUELT, // brukes hvis saksbehandler godtar avvik
    AVVIK, // parkeringsutgifter/for mange dager etc. saksbehandler må ta stilling til uka
    IKKE_MOTTATT_KJØRELISTE,
}

enum class TypeAvvikUke {
    FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK,
    OVERLAPPER_MED_ANNET_RAMMEVEDTAK,
}

enum class AvklartKjørtUkeStatus {
    NY, // Uke finnes ikke i forrige behandling
    ENDRET, // Uke finnes i forrige behandling, men er endret av saksbehandler (inkl. tømt innhold → gir 0 kr)
    UENDRET, // Uke er kopiert uendret fra forrige behandling
    SLETTET, // Uken er utenfor avkortet rammevedtak
}

fun Collection<AvklartKjørtUke>.finnesUkerMedAvvik() = this.any { uke -> uke.status == UkeStatus.AVVIK }

fun Collection<AvklartKjørtUke>.alleErUendret() = this.all { it.avklartKjørtUkeStatus == AvklartKjørtUkeStatus.UENDRET }
