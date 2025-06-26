package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserRegisterDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserUtil.finnRelevanteYtelsesTyper
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class YtelseService(
    private val fagsakPersonService: FagsakPersonService,
    private val ytelseClient: YtelseClient,
    private val behandlingService: BehandlingService,
) {
    fun hentYtelser(fagsakPersonId: FagsakPersonId): YtelserRegisterDto {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val typer =
            listOf(
                TypeYtelsePeriode.AAP,
                TypeYtelsePeriode.DAGPENGER,
                TypeYtelsePeriode.ENSLIG_FORSØRGER,
                TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                TypeYtelsePeriode.TILTAKSPENGER,
            )

        return ytelseClient
            .hentYtelser(
                YtelsePerioderRequest(
                    ident = ident,
                    fom = LocalDate.now().minusYears(3),
                    tom = LocalDate.now().plusYears(1),
                    typer = typer,
                ),
            ).tilDto()
    }

    fun hentYtelseForGrunnlag(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ): YtelsePerioderDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val typer = finnRelevanteYtelsesTyper(behandling.stønadstype)
        return hentYtelser(behandling.ident, fom, tom, typer)
    }

    fun hentYtelser(
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
        typer: List<TypeYtelsePeriode>,
    ): YtelsePerioderDto {
        feilHvis(typer.isEmpty()) {
            "Kan ikke hente ytelser uten å definiere typer"
        }
        val ytelsePerioder =
            ytelseClient.hentYtelser(
                YtelsePerioderRequest(
                    ident = ident,
                    fom = fom,
                    tom = tom,
                    typer = typer,
                ),
            )

        return ytelsePerioder.copy(
            perioder =
                ytelsePerioder.perioder
                    .filter { it.ensligForsørgerStønadstype != EnsligForsørgerStønadstype.BARNETILSYN },
        )
    }
}
