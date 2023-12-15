package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

data class Fagsaker(
    val barnetilsyn: Fagsak?,
)

data class Fagsak(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personIdenter: Set<PersonIdent>,
    val eksternId: EksternFagsakId,
    val stønadstype: Stønadstype,
    val sporbar: Sporbar,
) {

    fun erAktivIdent(personIdent: String): Boolean = hentAktivIdent() == personIdent

    fun hentAktivIdent(): String {
        return personIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident
            ?: error("Fant ingen ident på fagsak $id")
    }
}

@Table("fagsak")
data class FagsakDomain(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakPersonId: UUID,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("fagsak_ekstern")
data class EksternFagsakId(
    @Id
    val id: Long = 0,
    val fagsakId: UUID,
)

fun FagsakDomain.tilFagsakMedPerson(personIdenter: Set<PersonIdent>, eksternFagsakId: EksternFagsakId): Fagsak =
    Fagsak(
        id = id,
        fagsakPersonId = fagsakPersonId,
        personIdenter = personIdenter,
        eksternId = eksternFagsakId,
        stønadstype = stønadstype,
        sporbar = sporbar,
    )
