package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.kontrakter.felles.gjelderReiseTilSamling
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table

data class Fagsaker(
    private val fagsaker: Map<Stønadstype, Fagsak>,
) {
    val barnetilsyn: Fagsak? = fagsaker[Stønadstype.BARNETILSYN]
    val læremidler: Fagsak? = fagsaker[Stønadstype.LÆREMIDLER]
    val boutgifter: Fagsak? = fagsaker[Stønadstype.BOUTGIFTER]
    val dagligReiseTso: Fagsak? = fagsaker[Stønadstype.DAGLIG_REISE_TSO]
    val dagligReiseTsr: Fagsak? = fagsaker[Stønadstype.DAGLIG_REISE_TSR]
    val reiseTilSamlingTso: Fagsak? = fagsaker[Stønadstype.REISE_TIL_SAMLING_TSO]

    fun alleFagsaker() = fagsaker.values

    fun alleFagsakerMedUtbetalingPåGammeltFagområde() =
        alleFagsaker().filter {
            it.utbetalPåNyttFagområde != null &&
                !it.utbetalPåNyttFagområde
        }

    fun alleFagsakerAvStønadstypeUavhengigAvTema(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR -> fagsaker.values.filter { it.stønadstype.gjelderDagligReise() }
            Stønadstype.REISE_TIL_SAMLING_TSO -> fagsaker.values.filter { it.stønadstype.gjelderReiseTilSamling() }
            Stønadstype.BARNETILSYN,
            Stønadstype.LÆREMIDLER,
            Stønadstype.BOUTGIFTER,
            -> alleFagsaker().filter { it.stønadstype == stønadstype }
        }
}

data class Fagsak(
    val id: FagsakId,
    val fagsakPersonId: FagsakPersonId,
    val personIdenter: Set<PersonIdent>,
    val eksternId: EksternFagsakId,
    val stønadstype: Stønadstype,
    val utbetalPåNyttFagområde: Boolean? = null,
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
    @Column("utbetal_pa_nytt_fagomrade")
    val utbetalPåNyttFagområde: Boolean? = null,
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
        utbetalPåNyttFagområde = utbetalPåNyttFagområde,
        sporbar = sporbar,
    )
