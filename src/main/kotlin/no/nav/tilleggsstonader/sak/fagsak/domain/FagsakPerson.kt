package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.Endret
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDateTime
import java.util.UUID

data class FagsakPerson(
    @Id
    val id: UUID = UUID.randomUUID(),
    @MappedCollection(idColumn = "fagsak_person_id")
    val identer: Set<PersonIdent>,
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
) {

    fun hentAktivIdent(): String {
        return identer.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på person $id")
    }

    fun medOppdatertGjeldendeIdent(gjeldendePersonIdent: String): FagsakPerson {
        val personIdentForGjeldendeIdent: PersonIdent = this.identer.find { it.ident == gjeldendePersonIdent }?.let {
            it.copy(sporbar = it.sporbar.copy(endret = Endret()))
        } ?: PersonIdent(ident = gjeldendePersonIdent)
        val søkerIdenterUtenGjeldende = this.identer.filter { it.ident != gjeldendePersonIdent }

        return this.copy(identer = søkerIdenterUtenGjeldende.toSet() + personIdentForGjeldendeIdent)
    }
}

data class PersonIdent(
    @Id
    val ident: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
