package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import java.time.LocalDate

object GeneriskFaktaGrunnlagTestUtil {
    fun faktaGrunnlagBarnAnnenForelder(
        behandlingId: BehandlingId = BehandlingId.random(),
        identBarn: String = "barn1",
        annenForelder: List<FaktaGrunnlagAnnenForelderSaksinformasjon> = listOf(faktaGrunnlagAnnenForelder()),
    ) = GeneriskFaktaGrunnlag(
        behandlingId = behandlingId,
        data =
            FaktaGrunnlagBarnAndreForeldreSaksinformasjon(
                identBarn = identBarn,
                andreForeldre = annenForelder,
            ),
        typeId = identBarn,
        sporbar = Sporbar(opprettetAv = "id123", opprettetTid = LocalDate.of(2025, 1, 2).atStartOfDay()),
    )

    fun faktaGrunnlagAnnenForelder(
        ident: String = "forelder1",
        harBehandlingUnderArbeid: Boolean = true,
        oppfyltePerioderForBarn: List<Datoperiode> = listOf(Datoperiode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))),
    ) = FaktaGrunnlagAnnenForelderSaksinformasjon(
        ident = ident,
        harBehandlingUnderArbeid = harBehandlingUnderArbeid,
        vedtaksperioderBarn = oppfyltePerioderForBarn,
    )
}
