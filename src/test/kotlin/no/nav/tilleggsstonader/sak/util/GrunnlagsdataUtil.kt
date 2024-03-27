package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
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
        barn: List<GrunnlagBarn> = listOf(lagGrunnlagsdataBarn()),
    ) = Grunnlag(
        barn = barn,
    )

    fun lagGrunnlagsdataBarn(
        ident: String = "1",
        navn: Navn = PdlTestdataHelper.lagNavn(),
        alder: Int? = 6,
        dødsdato: LocalDate? = null,
    ) = GrunnlagBarn(
        ident = ident,
        navn = navn,
        alder = alder,
        dødsdato = dødsdato,
    )
}
