package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserRegisterDtoMapper.tilDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class YtelseService(
    private val fagsakPersonService: FagsakPersonService,
    private val ytelseClient: YtelseClient,
    private val behandlingService: BehandlingService,
) {
    fun hentYtelser(fagsakPersonId: UUID): YtelserRegisterDto {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return ytelseClient.hentYtelser(
            YtelsePerioderRequest(
                ident = ident,
                fom = osloDateNow().minusYears(3),
                tom = osloDateNow().plusYears(1),
                typer = listOf(TypeYtelsePeriode.AAP, TypeYtelsePeriode.ENSLIG_FORSØRGER),
            ),
        ).tilDto()
    }

    fun hentYtelserForBehandling(behandlingId: UUID): YtelserRegisterDto {
        val ident = behandlingService.hentSaksbehandling(behandlingId).ident

        return ytelseClient.hentYtelser(
            YtelsePerioderRequest(
                ident = ident,
                fom = osloDateNow().minusMonths(3),
                tom = osloDateNow().plusYears(1),
                typer = listOf(TypeYtelsePeriode.AAP, TypeYtelsePeriode.ENSLIG_FORSØRGER),
            ),
        ).tilDto()
    }
}
