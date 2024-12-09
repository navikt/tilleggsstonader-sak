package no.nav.tilleggsstonader.sak.opplysninger

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.util.VirtualThreadUtil.parallelt
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(path = ["/api/aap-sjekk"])
@ProtectedWithClaims(issuer = "azuread")
class AAPSjekk(
    private val behandlingService: BehandlingService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakService: VedtakService,
    private val ytelseClient: YtelseClient,
) {

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    @GetMapping
    fun hent(): List<Map<String, Any>> {
        utførEndringSomSystem()
        val alleBehandlinger = behandlingRepository.findAll()
        val response = alleBehandlinger
            .filter { it.erAvsluttet() }
            .filter { it.resultat == BehandlingResultat.INNVILGET || it.resultat == BehandlingResultat.OPPHØRT }
            .groupBy { it.fagsakId }
            .mapValues { it.value.minByOrNull { it.vedtakstidspunkt!! }!! }
            .values.chunked(5).flatMap {
                it.map { finnAvvik(it) }.parallelt()
            }
        secureLogger.info("Antall avvik=${response.size}")
        return response
    }

    private fun finnAvvik(behandling: Behandling): () -> Map<String, Any> = {
        val perioder = perioderFraVedtak(behandling.id)
        if (perioder.isNotEmpty()) {
            val saksbehandling = behandlingService.hentSaksbehandling(behandling.id)
            val ytelser = hentYtelser(perioder, saksbehandling)
            val ytelseInneholderVedtaksperiode = perioder.any { periode ->
                val ikkeFerdigAvklart = ytelser.filter { !it.ferdigAvklart }
                ikkeFerdigAvklart.any { ytelse -> ytelse.inneholder(periode) }
            }
            if (!ytelseInneholderVedtaksperiode) {
                mapOf(
                    "behandlingId" to behandling.id,
                    "sakId" to saksbehandling.eksternId.toString(),
                    "vedtaksperioder" to perioder,
                    "ytelsesperioder" to ytelser,
                )
            } else {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    private fun hentYtelser(
        perioder: List<SPeriode>,
        saksbehandling: Saksbehandling,
    ): List<AAPPeriode> {
        val fom = perioder.minOf { it.fom }
        val tom = perioder.maxOf { it.tom }
        val perioder = ytelseClient.hentYtelser(
            YtelsePerioderRequest(
                ident = saksbehandling.ident,
                fom = fom,
                tom = tom,
                typer = listOf(TypeYtelsePeriode.AAP),
            ),
        )
        if (perioder.hentetInformasjon.any { it.status == StatusHentetInformasjon.FEILET }) {
            secureLogger.info("Ytelser for behandling=${saksbehandling.id} feilet")
        }
        return perioder.perioder
            .map { AAPPeriode(fom = it.fom, tom = it.tom!!, ferdigAvklart = it.aapErFerdigAvklart ?: false) }
            .mergeSammenhengende(
                { a, b -> a.overlapperEllerPåfølgesAv(b) && a.ferdigAvklart == b.ferdigAvklart },
                { a, b -> a.copy(fom = minOf(a.fom, b.fom), tom = maxOf(a.tom, b.tom)) },
            )
            .sorted()
    }

    private fun perioderFraVedtak(
        behandlingId: BehandlingId,
    ): List<SPeriode> {
        val vedtak = vedtakService.hentVedtak<VedtakTilsynBarn>(behandlingId)
        require(vedtak != null) { "Vedtak for behandlingId=$behandlingId er null" }
        val beregningsresultat = when (vedtak.data) {
            is AvslagTilsynBarn -> error("Avslag for behandlingId=$behandlingId")
            is InnvilgelseTilsynBarn -> vedtak.data.beregningsresultat
            is OpphørTilsynBarn -> vedtak.data.beregningsresultat
        }
        val perioder = beregningsresultat.perioder.flatMap {
            it.grunnlag.stønadsperioderGrunnlag
                .filter { it.stønadsperiode.målgruppe == MålgruppeType.AAP }
                .map { SPeriode(it.stønadsperiode.fom, it.stønadsperiode.tom) }
        }
        return perioder.mergeSammenhengende(
            { a, b -> a.overlapperEllerPåfølgesAv(b) },
            { a, b -> SPeriode(fom = minOf(a.fom, b.fom), tom = maxOf(a.tom, b.tom)) },
        )
            .sorted()
    }

    data class SPeriode(
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : Periode<LocalDate>

    data class AAPPeriode(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val ferdigAvklart: Boolean,
    ) : Periode<LocalDate>
}
