package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import java.time.LocalDate
import java.time.LocalDateTime

object GrunnlagsdataUtil {
    fun grunnlagsdataMedMetadata(
        grunnlagsdata: Grunnlagsdata = lagGrunnlagsdata(),
        opprettetTidspunkt: LocalDateTime = LocalDate.of(2010, 1, 1).atStartOfDay(),
    ) = GrunnlagsdataMedMetadata(
        grunnlagsdata = grunnlagsdata,
        opprettetTidspunkt = opprettetTidspunkt,
    )

    fun lagGrunnlagsdata(
        barn: List<GrunnlagsdataBarn> = listOf(lagGrunnlagsdataBarn()),
    ) = Grunnlagsdata(
        barn = barn,
    )

    fun lagGrunnlagsdataBarn(
        ident: String = "1",
        navn: Navn = PdlTestdataHelper.lagNavn(),
        fødselsdato: LocalDate? = PdlTestdataHelper.fødsel().fødselsdato,
        dødsdato: LocalDate? = null,
    ) = GrunnlagsdataBarn(
        ident = ident,
        navn = navn,
        fødselsdato = fødselsdato,
        dødsdato = dødsdato,
    )
}
