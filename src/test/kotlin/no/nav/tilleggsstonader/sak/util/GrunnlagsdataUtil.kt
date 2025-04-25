package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagArena
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.Navn
import java.time.LocalDate

object GrunnlagsdataUtil {
    fun lagGrunnlagsdata(
        navn: Navn = lagNavn(),
        fødsel: FødselFaktaGrunnlag? = FødselFaktaGrunnlag(fødselsdato = LocalDate.of(2000, 1, 1), fødselsår = 2000),
        barn: List<GrunnlagBarn> = listOf(lagGrunnlagsdataBarn()),
        arena: GrunnlagArena? = GrunnlagArena(vedtakTom = LocalDate.of(2024, 1, 1)),
    ) = Grunnlag(
        navn = navn,
        fødsel = fødsel,
        barn = barn,
        arena = arena,
    )

    fun lagFødselFaktaGrunnlag(fødselsdato: LocalDate? = LocalDate.of(2000, 1, 1)) =
        FødselFaktaGrunnlag(fødselsdato = fødselsdato, fødselsår = fødselsdato?.year ?: 2000)

    fun lagGrunnlagsdataBarn(
        ident: String = "1",
        navn: Navn = lagNavn(),
        fødselsdato: LocalDate? = LocalDate.of(2024, 6, 4),
        dødsdato: LocalDate? = null,
    ) = GrunnlagBarn(
        ident = ident,
        navn = navn,
        alder = antallÅrSiden(fødselsdato),
        fødselsdato = fødselsdato,
        dødsdato = dødsdato,
    )

    fun lagNavn(
        fornavn: String = "Fornavn",
        mellomnavn: String? = "mellomnavn",
        etternavn: String = "Etternavn",
    ): Navn =
        Navn(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
        )
}
