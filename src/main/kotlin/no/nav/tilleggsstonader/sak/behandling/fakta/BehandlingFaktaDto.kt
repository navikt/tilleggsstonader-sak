package no.nav.tilleggsstonader.sak.behandling.fakta

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakOppholdUtenforNorge
import java.time.LocalDate
import java.util.UUID

data class BehandlingFaktaDto(
    val hovedytelse: FaktaHovedytelse,
    val aktivitet: FaktaAktivtet,
    val barn: List<FaktaBarn>,
    val dokumentasjon: FaktaDokumentasjon?,
)

data class FaktaHovedytelse(
    val søknadsgrunnlag: SøknadsgrunnlagHovedytelse?,
)

data class SøknadsgrunnlagHovedytelse(
    val hovedytelse: List<Hovedytelse>,
    val arbeidOgOpphold: FaktaArbeidOgOpphold?,
)

data class FaktaArbeidOgOpphold(
    val jobberIAnnetLand: JaNei?,
    val jobbAnnetLand: String?,
    val harPengestøtteAnnetLand: List<TypePengestøtte>?,
    val pengestøtteAnnetLand: String?,
    val harOppholdUtenforNorgeSiste12mnd: JaNei?,
    val oppholdUtenforNorgeSiste12mnd: List<FaktaOppholdUtenforNorge>,
    val harOppholdUtenforNorgeNeste12mnd: JaNei?,
    val oppholdUtenforNorgeNeste12mnd: List<FaktaOppholdUtenforNorge>,
)

data class FaktaOppholdUtenforNorge(
    val land: String,
    val årsak: List<ÅrsakOppholdUtenforNorge>,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class FaktaAktivtet(
    val søknadsgrunnlag: SøknadsgrunnlagAktivitet?,
)

data class SøknadsgrunnlagAktivitet(
    val utdanning: JaNei,
)

data class FaktaBarn(
    val ident: String,
    val barnId: UUID,
    val registergrunnlag: RegistergrunnlagBarn,
    val søknadgrunnlag: SøknadsgrunnlagBarn?,
)

data class FaktaDokumentasjon(
    val journalpostId: String,
    val dokumentasjon: List<Dokumentasjon>,
)

data class Dokumentasjon(
    val type: String,
    val dokumenter: List<Dokument>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val identBarn: String? = null,
)

data class Dokument(val dokumentInfoId: String)

data class RegistergrunnlagBarn(
    val navn: String,
    val alder: Int?,
    val dødsdato: LocalDate?,
)

data class SøknadsgrunnlagBarn(
    val type: TypeBarnepass,
    val startetIFemte: JaNei?,
    val årsak: ÅrsakBarnepass?,
)
