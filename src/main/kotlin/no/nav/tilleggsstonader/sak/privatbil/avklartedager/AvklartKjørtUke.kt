package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
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
    @Column("kjoreliste_id")
    val kjørelisteId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val ukenummer: Int,
    val status: UkeStatus,
    val typeAvvik: TypeAvvikUke? = null,
    val behandletDato: LocalDate? = null,
    @MappedCollection(idColumn = "avklart_kjort_uke_id")
    val dager: Set<AvklartKjørtDag>,
) : Periode<LocalDate> {
    init {
        require(dager.all { inneholder(it.dato) }) { "Alle dager må være innenfor perioden til uken" }
        require(fom.ukenummer() == tom.ukenummer()) { "Fom og tom må være i samme uke" }
        require(fom.ukenummer() == ukenummer) { "Ukenummer $ukenummer stemmer ikke med perioden" }
    }
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
