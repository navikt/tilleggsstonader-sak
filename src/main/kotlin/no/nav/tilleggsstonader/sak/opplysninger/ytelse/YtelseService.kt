package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.familie.prosessering.rest.RestTaskService.Companion.logger
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserRegisterDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserUtil.filtrerAktivitetspengerHvisDeaktivert
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserUtil.finnRelevanteYtelsesTyper
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class YtelseService(
    private val fagsakPersonService: FagsakPersonService,
    private val ytelseClient: YtelseClient,
    private val behandlingService: BehandlingService,
    private val unleashService: UnleashService,
) {
    fun hentYtelser(fagsakPersonId: FagsakPersonId): YtelserRegisterDto {
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val typer =
            listOf(
                TypeYtelsePeriode.AAP,
                TypeYtelsePeriode.DAGPENGER,
                TypeYtelsePeriode.ENSLIG_FORSØRGER,
                TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                TypeYtelsePeriode.TILTAKSPENGER_TPSAK,
                TypeYtelsePeriode.TILTAKSPENGER_ARENA,
                TypeYtelsePeriode.AKTIVITETSPENGER,
            ).filtrerAktivitetspengerHvisDeaktivert()

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

    fun harAktivTsrMålgruppe(ident: String): Boolean {
        val typer =
            listOf(
                TypeYtelsePeriode.DAGPENGER,
                TypeYtelsePeriode.TILTAKSPENGER_TPSAK,
                TypeYtelsePeriode.TILTAKSPENGER_ARENA,
            )

        val aktiveTsrVedtak =
            ytelseClient
                .hentYtelser(
                    YtelsePerioderRequest(
                        ident = ident,
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        typer = typer,
                    ),
                )

        return aktiveTsrVedtak.perioder.isNotEmpty()
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

    /**
     * Returnerer false hvis kallet mot ung-sak feiler, slik at bruker kan svare selv på om de mottar aktivitetspenger.
     * Bruk [harAktivitetspengerIDagEllerFremITidEllerFeil] når resultatet lagres uten bekreftelse fra bruker.
     */
    fun harAktivitetspengerIDagEllerFremITid(ident: String): Boolean = finnAktivitetspengerIDagEllerFremITid(ident) ?: false

    /**
     * Skiller feilet oppslag fra bekreftet nei, slik at oppretting kan feile og prøves på nytt.
     */
    fun harAktivitetspengerIDagEllerFremITidEllerFeil(ident: String): Boolean =
        finnAktivitetspengerIDagEllerFremITid(ident)
            ?: feil("Kunne ikke avgjøre om bruker har aktivitetspenger fordi kall mot ung-sak feilet")

    /**
     * Returnerer null hvis kallet mot ung-sak feiler.
     */
    private fun finnAktivitetspengerIDagEllerFremITid(ident: String): Boolean? {
        val ytelser =
            try {
                ytelseClient.hentYtelser(
                    YtelsePerioderRequest(
                        ident = ident,
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusYears(1),
                        typer = listOf(TypeYtelsePeriode.AKTIVITETSPENGER),
                    ),
                )
            } catch (e: Exception) {
                logger.warn("Kall for å hente aktivitetspenger feilet", e)
                return null
            }

        val kallFeilet =
            ytelser.kildeResultat.any {
                it.type == TypeYtelsePeriode.AKTIVITETSPENGER && it.resultat == ResultatKilde.FEILET
            }
        if (kallFeilet) {
            logger.warn("Henting av aktivitetspenger fra ung-sak feilet")
            return null
        }

        return ytelser.perioder.any { it.type == TypeYtelsePeriode.AKTIVITETSPENGER }
    }

    fun hentYtelseForGrunnlag(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ): YtelsePerioderDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val typer = finnRelevanteYtelsesTyper(behandling.stønadstype).filtrerAktivitetspengerHvisDeaktivert()
        return hentYtelser(behandling.ident, fom, tom, typer)
    }

    private fun List<TypeYtelsePeriode>.filtrerAktivitetspengerHvisDeaktivert(): List<TypeYtelsePeriode> =
        filtrerAktivitetspengerHvisDeaktivert(unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER))

    fun hentYtelser(
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
        typer: List<TypeYtelsePeriode>,
    ): YtelsePerioderDto {
        feilHvis(typer.isEmpty()) {
            "Kan ikke hente ytelser uten å definere typer"
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
                    .filterNot {
                        it is YtelsePeriode.EnsligForsørger &&
                            it.ensligForsørgerStønadstype == EnsligForsørgerStønadstype.BARNETILSYN
                    },
        )
    }
}
