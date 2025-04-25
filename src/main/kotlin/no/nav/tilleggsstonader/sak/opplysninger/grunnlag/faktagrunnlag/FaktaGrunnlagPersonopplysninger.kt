package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.util.antallÅrSiden
import java.time.LocalDate

data class FaktaGrunnlagPersonopplysninger(
    val navn: Navn,
    val fødsel: Fødsel?,
    val barn: List<GrunnlagBarn>,
) : FaktaGrunnlagData {
    override val type: TypeFaktaGrunnlag = TypeFaktaGrunnlag.PERSONOPPLYSNINGER

    companion object {
        fun fraSøkerMedBarn(
            person: SøkerMedBarn,
            barnPåBehandling: List<BehandlingBarn>,
        ): FaktaGrunnlagPersonopplysninger =
            FaktaGrunnlagPersonopplysninger(
                navn =
                    person.søker.navn
                        .gjeldende()
                        .tilNavn(),
                fødsel = Fødsel.fraSøkerMedBarn(person),
                barn = GrunnlagBarn.fraSøkerMedBarn(person, barnPåBehandling),
            )
    }
}

data class Fødsel(
    val fødselsdato: LocalDate?,
    val fødselsår: Int,
) {
    fun fødselsdatoEller1JanForFødselsår() =
        fødselsdato
            ?: LocalDate.of(fødselsår, 1, 1)

    companion object {
        fun fraSøkerMedBarn(person: SøkerMedBarn): Fødsel {
            val fødsel = person.søker.fødselsdato.gjeldende()
            return Fødsel(
                fødselsdato = fødsel.fødselsdato,
                fødselsår = fødsel.fødselsår ?: error("Forventer at fødselsår skal finnes på alle brukere"),
            )
        }
    }
}

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
) {
    fun visningsnavn(): String =
        if (mellomnavn == null) {
            "$fornavn $etternavn"
        } else {
            "$fornavn $mellomnavn $etternavn"
        }
}

data class GrunnlagBarn(
    val ident: String,
    val navn: Navn,
    val fødselsdato: LocalDate?,
    val alder: Int?,
    val dødsdato: LocalDate?,
) {
    companion object {
        fun fraSøkerMedBarn(
            person: SøkerMedBarn,
            barnPåBehandling: List<BehandlingBarn>,
        ): List<GrunnlagBarn> {
            val barnIdenter = barnPåBehandling.map { it.ident }.toSet()
            val barn = person.barn.filter { (ident, _) -> barnIdenter.contains(ident) }

            feilHvis(
                !barn.keys.containsAll(barnIdenter),
                sensitivFeilmelding = { "Finner ikke grunnlag for barn. behandlingBarn=$barnIdenter pdlBarn=${barn.keys}" },
            ) {
                "Finner ikke grunnlag for barn. Se securelogs for detaljer."
            }
            return barn.map { (ident, barn) ->
                GrunnlagBarn(
                    ident = ident,
                    navn = barn.navn.gjeldende().tilNavn(),
                    fødselsdato = barn.fødselsdato.gjeldende().fødselsdato,
                    alder = antallÅrSiden(barn.fødselsdato.gjeldende().fødselsdato),
                    dødsdato = barn.dødsfall.gjeldende()?.dødsdato,
                )
            }
        }
    }
}

fun no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn.tilNavn() =
    Navn(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
    )
