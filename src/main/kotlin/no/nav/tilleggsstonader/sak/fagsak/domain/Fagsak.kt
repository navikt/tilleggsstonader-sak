package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table

data class Fagsaker(
    val barnetilsyn: Fagsak?,
    val læremidler: Fagsak?,
    val boutgifter: Fagsak?,
    val dagligReiseTSO: Fagsak?,
    val dagligReiseTSR: Fagsak?,
)

data class Fagsak(
    val id: FagsakId,
    val fagsakPersonId: FagsakPersonId,
    val personIdenter: Set<PersonIdent>,
    val eksternId: EksternFagsakId,
    val stønadstype: Stønadstype,
    val sporbar: Sporbar,
) {
    fun erAktivIdent(personIdent: String): Boolean = hentAktivIdent() == personIdent

    fun hentAktivIdent(): String =
        personIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident
            ?: error("Fant ingen ident på fagsak $id")
}

@Table("fagsak")
data class FagsakDomain(
    @Id
    val id: FagsakId = FagsakId.random(),
    val fagsakPersonId: FagsakPersonId,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("fagsak_ekstern")
data class EksternFagsakId(
    @Id
    val id: Long = 0,
    val fagsakId: FagsakId,
)

fun FagsakDomain.tilFagsakMedPerson(
    personIdenter: Set<PersonIdent>,
    eksternFagsakId: EksternFagsakId,
): Fagsak =
    Fagsak(
        id = id,
        fagsakPersonId = fagsakPersonId,
        personIdenter = personIdenter,
        eksternId = eksternFagsakId,
        stønadstype = stønadstype,
        sporbar = sporbar,
    )
