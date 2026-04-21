package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
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
    val typeAvvik: TypeAvvikUke? = null,
    val behandletDato: LocalDate? = null,
    @MappedCollection(idColumn = "avklart_kjort_uke_id")
    val dager: Set<AvklartKjørtDag>,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
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
            dager = dager.map { it.copy(id = UUID.randomUUID()) }.toSet(),
        )
}

enum class UkeStatus {
    OK_AUTOMATISK, // brukes hvis automatisk godkjent
    OK_MANUELT, // brukes hvis saksbehandler godtar avvik
    AVVIK, // parkeringsutgifter/for mange dager etc. saksbehandler må ta stilling til uka
    IKKE_MOTTATT_KJØRELISTE,
}

enum class TypeAvvikUke {
    FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK,
}

fun Collection<AvklartKjørtUke>.finnesUkerMedAvvik() = this.any { uke -> uke.status == UkeStatus.AVVIK }
