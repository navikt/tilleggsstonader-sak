package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.familie.prosessering.rest.RestTaskService.Companion.logger
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

    fun harAktivtAapVedtak(ident: String): HarAktivtVedtakDto {
        val aktiveAapVedtak =
            ytelseClient
                .hentYtelser(
                    YtelsePerioderRequest(
                        ident = ident,
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        typer = listOf(TypeYtelsePeriode.AAP),
                    ),
                )

        feilHvis(aktiveAapVedtak.perioder.size > 1) {
            "Forventer maks én periode ettersom perioden vi etterspør er én spesifikk dato"
        }
        feilHvis(aktiveAapVedtak.kildeResultat.isEmpty()) {
            error(
                "Forventer ett og bare ett resultat, ettersom vi spør om én enkelt ytelse og det ikke gir mening å ha flere aktive AAP-vedtak samtidig",
            )
        }
        val resultatType = aktiveAapVedtak.kildeResultat.single().type
        feilHvis(resultatType != TypeYtelsePeriode.AAP) {
            "Etterspurte AAP, men fikk resultat fra $resultatType"
        }

        val harAktivtAapVedtak = aktiveAapVedtak.perioder.isNotEmpty()
        val kalletsStatus = aktiveAapVedtak.kildeResultat.first().resultat

        logger.info("Hentet AAP-status fra Arena. resultat='$aktiveAapVedtak'")

        return HarAktivtVedtakDto(
            type = TypeYtelsePeriode.AAP,
            harAktivtVedtak = harAktivtAapVedtak,
            resultatKilde = kalletsStatus,
        )
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
