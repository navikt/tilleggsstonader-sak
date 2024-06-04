package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Fødsel
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagArena
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Navn
import java.time.LocalDate
import java.util.UUID

object GrunnlagsdataUtil {
    fun grunnlagsdataDomain(
        behandlingId: UUID = UUID.randomUUID(),
        grunnlag: Grunnlag = lagGrunnlagsdata(),
    ) = Grunnlagsdata(
        behandlingId = behandlingId,
        grunnlag = grunnlag,
    )

    fun lagGrunnlagsdata(
        navn: Navn = lagNavn(),
        fødsel: Fødsel? = Fødsel(fødselsdato = LocalDate.of(2000, 1, 1), fødselsår = 2000),
        barn: List<GrunnlagBarn> = listOf(lagGrunnlagsdataBarn()),
        arena: GrunnlagArena? = GrunnlagArena(vedtakTom = LocalDate.of(2024, 1, 1)),
    ) = Grunnlag(
        navn = navn,
        fødsel = fødsel,
        barn = barn,
        arena = arena,
    )

    fun lagGrunnlagsdataBarn(
        ident: String = "1",
        navn: Navn = lagNavn(),
        fødselsdato: LocalDate = osloDateNow(),
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
    ): Navn {
        return Navn(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
        )
    }
}
