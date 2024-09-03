package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.HentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserRegisterDtoMapper.tilDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class YtelseService(
    private val fagsakPersonService: FagsakPersonService,
    private val ytelseClient: YtelseClient,
    private val behandlingService: BehandlingService,
) {
    fun hentYtelser(fagsakPersonId: UUID): YtelserRegisterDto {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val typer = listOf(
            TypeYtelsePeriode.AAP,
            TypeYtelsePeriode.ENSLIG_FORSØRGER,
            TypeYtelsePeriode.OMSTILLINGSSTØNAD,
        )

        return ytelseClient.hentYtelser(
            YtelsePerioderRequest(
                ident = ident,
                fom = osloDateNow().minusYears(3),
                tom = osloDateNow().plusYears(1),
                typer = typer,
            ),
        ).tilDto()
    }

    fun hentYtelseForGrunnlag(
        behandlingId: UUID,
        fom: LocalDate,
        tom: LocalDate,
    ): YtelsePerioderDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val typer = finnRelevanteYtelsesTyper(behandling.stønadstype)

        val ytelsePerioder = ytelseClient.hentYtelser(
            YtelsePerioderRequest(
                ident = behandling.ident,
                fom = fom,
                tom = tom,
                typer = typer,
            ),
        )

        validerResultat(ytelsePerioder.hentetInformasjon)

        return ytelsePerioder
    }

    private fun validerResultat(hentetInformasjon: List<HentetInformasjon>) {
        val test = hentetInformasjon.filter { it.status != StatusHentetInformasjon.OK }

        feilHvis(test.isNotEmpty()) {
            "Feil ved henting av ytelser fra andre systemer: ${test.joinToString(", ") { it.type.name }}. Prøv å laste inn siden på nytt."
        }
    }

    private fun finnRelevanteYtelsesTyper(type: Stønadstype) =
        when (type) {
            Stønadstype.BARNETILSYN ->
                listOf(
                    TypeYtelsePeriode.AAP,
                    TypeYtelsePeriode.ENSLIG_FORSØRGER,
                    TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                )
            else -> error("Finner ikke relevante ytelser for stønadstype $type")
        }
}
