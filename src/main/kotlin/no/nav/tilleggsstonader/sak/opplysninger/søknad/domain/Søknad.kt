package no.nav.tilleggsstonader.sak.opplysninger.søknad.domain

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface Søknad<T> {
    val id: UUID
    val journalpostId: String
    val mottattTidspunkt: LocalDateTime
    val språk: Språkkode
    val sporbar: Sporbar
    val data: T
}

/**
 * Gjør det mulig å koble en søknad til flere behandlinger
 */
@Table("soknad_behandling")
data class SøknadBehandling(
    @Id
    val behandlingId: UUID,
    @Column("soknad_id")
    val søknadId: UUID,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("soknad")
data class SøknadBarnetilsyn(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val journalpostId: String,
    override val mottattTidspunkt: LocalDateTime,
    @Column("sprak")
    override val språk: Språkkode,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    override val sporbar: Sporbar = Sporbar(),
    override val data: SkjemaBarnetilsyn,

    @MappedCollection(idColumn = "soknad_id")
    val barn: Set<SøknadBarn>,
) : Søknad<SkjemaBarnetilsyn>

data class SkjemaBarnetilsyn(
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetAvsnitt,
    val dokumentasjon: List<Dokumentasjon>,
)

data class HovedytelseAvsnitt(
    val hovedytelse: List<Hovedytelse>,
    val arbeidOgOpphold: ArbeidOgOpphold?,
)

data class ArbeidOgOpphold(
    val jobberIAnnetLand: JaNei?,
    val jobbAnnetLand: String?,
    val harPengestøtteAnnetLand: List<TypePengestøtte>?,
    val pengestøtteAnnetLand: String?,
    val harOppholdUtenforNorgeSiste12mnd: JaNei?,
    val oppholdUtenforNorgeSiste12mnd: List<OppholdUtenforNorge>,
    val harOppholdUtenforNorgeNeste12mnd: JaNei?,
    val oppholdUtenforNorgeNeste12mnd: List<OppholdUtenforNorge>,
)

data class OppholdUtenforNorge(
    val land: String,
    val årsak: List<ÅrsakOppholdUtenforNorge>,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class AktivitetAvsnitt(
    val utdanning: JaNei,
)

data class Dokumentasjon(
    val type: Vedleggstype,
    val dokumenter: List<Dokument>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val identBarn: String? = null,
)

data class Dokument(val dokumentInfoId: String)
