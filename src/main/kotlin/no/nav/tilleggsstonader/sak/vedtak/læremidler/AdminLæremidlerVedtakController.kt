package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.slåSammenSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BrukVedtaksperioderForBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.ResultSet
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/vedtak/admin/laremidler")
@ProtectedWithClaims(issuer = "azuread")
class AdminLæremidlerVedtakController(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vedtakService: VedtakService,
    private val vedtakRepository: VedtakRepository,
    private val behandlingService: BehandlingService,
    private val beregningService: LæremidlerBeregningService,
    private val transactionHandler: TransactionHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun testVedtaksperioder() {
        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            try {
                kjørOppdatering(oppdater = false)
            } catch (e: Throwable) {
                logger.warn("Feil ved oppdatering av vedtak. Se securelogs")
                secureLogger.error("Feil ved oppdatering av vedtak", e)
            }
        }
    }

    @PostMapping("oppdater")
    fun oppdater() {
        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            try {
                kjørOppdatering(oppdater = true)
            } catch (e: Throwable) {
                logger.warn("Feil ved oppdatering av vedtak. Se securelogs")
                secureLogger.error("Feil ved oppdatering av vedtak", e)
            }
        }
    }

    fun kjørOppdatering(oppdater: Boolean) {
        val behandlinger = hentBehandlingerForLæremidler()
        logger.info("Finner ${behandlinger.size} behandlinger")
        transactionHandler.runInTransaction {
            behandlinger.forEach { behandling ->
                logger.info("Behandler ${behandling.behandlingId}")
                vedtakService.hentVedtak(behandling.behandlingId)?.let { vedtak ->
                    behandleVedtak(behandling.behandlingId, vedtak, oppdater)
                }
            }
        }
        logger.info("Jobb ferdig")
    }

    private fun behandleVedtak(
        behandlingId: BehandlingId,
        vedtak: Vedtak,
        oppdater: Boolean,
    ) {
        val vedtakdata = vedtak.data
        when (vedtakdata) {
            is InnvilgelseLæremidler,
            is OpphørLæremidler,
            -> behandleInnvilgelseEllerOpphør(behandlingId, vedtakdata, oppdater, vedtak)

            else ->
                logger.info(
                    "Oppdatering av vedtaksperioder læremidler behandling=$behandlingId result=FEIL_TYPE type=${vedtak::class.simpleName}",
                )
        }
    }

    private fun behandleInnvilgelseEllerOpphør(
        behandlingId: BehandlingId,
        vedtakdata: InnvilgelseEllerOpphørLæremidler,
        oppdater: Boolean,
        vedtak: Vedtak,
    ) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val stønadsperioder = hentStønadsperioder(behandlingId)
        val vedtaksperioder = vedtakdata.vedtaksperioder
        if (vedtaksperioder.isEmpty()) {
            logger.info("Oppdatering av vedtaksperioder læremidler behandling=$behandlingId result=IGNORE - ingen vedtaksperioder")
            return
        }
        if (vedtaksperioder.any { it.målgruppe != null || it.aktivitet != null }) {
            logger.info(
                "Oppdatering av vedtaksperioder læremidler behandling=$behandlingId result=IGNORE - har vedtaksperiode med målgruppe eller aktivitet",
            )
            return
        }
        val vedtaksperioderMedMålgruppeOgAktivitet =
            vedtaksperioder
                .map { vedtaksperiode ->
                    val stønadsperiode = stønadsperioder.singleOrNull { it.inneholder(vedtaksperiode) }
                    feilHvis(stønadsperiode == null) {
                        "Finner ikke vedtaksperiode som overlapper stønadsperiode for behandling=$behandlingId"
                    }
                    vedtaksperiode.copy(målgruppe = stønadsperiode.målgruppe, aktivitet = stønadsperiode.aktivitet)
                }.sorted()
        val beregningOk =
            kontrollerBeregning(behandling, vedtaksperioderMedMålgruppeOgAktivitet, vedtakdata, behandlingId)
        if (beregningOk) {
            logger.info("Oppdatering av vedtaksperioder læremidler behandling=$behandlingId result=OK")
            if (oppdater) {
                oppdaterVedtaksperioder(vedtak, vedtaksperioderMedMålgruppeOgAktivitet)
            }
        }
    }

    private fun kontrollerBeregning(
        behandling: Saksbehandling,
        vedtaksperioderMedMålgruppeOgAktivitet: List<Vedtaksperiode>,
        vedtakdata: InnvilgelseEllerOpphørLæremidler,
        behandlingId: BehandlingId,
    ): Boolean {
        if (vedtakdata is OpphørLæremidler || behandling.forrigeIverksatteBehandlingId != null) {
            return true
        }
        val beregningsresultat =
            beregningService.beregn(
                behandling,
                vedtaksperioderMedMålgruppeOgAktivitet,
                brukVedtaksperioderForBeregning = BrukVedtaksperioderForBeregning(true),
            )
        val beregningsresultatErOk = beregningsresultat == vedtakdata.beregningsresultat
        if (!beregningsresultatErOk) {
            logger.warn("Oppdatering av vedtaksperioder læremidler behandling=$behandlingId result=FEIL se securelogs for mer info")
            secureLogger.warn(
                "Ulikt beregningsresultat for behandling=$behandlingId, " +
                    "forrige=${objectMapper.writeValueAsString(vedtakdata.beregningsresultat)} " +
                    "nytt=${objectMapper.writeValueAsString(beregningsresultat)}",
            )
        }
        return beregningsresultatErOk
    }

    private fun oppdaterVedtaksperioder(
        vedtak: Vedtak,
        vedtaksperioderMedMålgruppeOgAktivitet: List<Vedtaksperiode>,
    ) {
        val oppdatertVedtak =
            when (vedtak.data) {
                is InnvilgelseLæremidler ->
                    vedtak.withTypeOrThrow<InnvilgelseLæremidler>().copy(
                        data = vedtak.data.copy(vedtaksperioder = vedtaksperioderMedMålgruppeOgAktivitet),
                    )

                is OpphørLæremidler ->
                    vedtak.withTypeOrThrow<OpphørLæremidler>().copy(
                        data = vedtak.data.copy(vedtaksperioder = vedtaksperioderMedMålgruppeOgAktivitet),
                    )

                else -> error("Feil type vedtak=${vedtak::class.simpleName}")
            }
        vedtakRepository.update(oppdatertVedtak)
    }

    private fun hentStønadsperioder(behandlingId: BehandlingId): List<StønadsperiodeBeregningsgrunnlag> =
        stønadsperiodeRepository
            .findAllByBehandlingId(behandlingId)
            .tilSortertStønadsperiodeBeregningsgrunnlag()
            .slåSammenSammenhengende()

    private fun hentBehandlingerForLæremidler(): List<BehandlingInfo> =
        jdbcTemplate.query(
            """SELECT f.id fagsak_id, b.id behandling_id FROM fagsak f 
                    |join behandling b on b.fagsak_id = f.id 
                    |join vedtak v on v.behandling_id = b.id
                    |WHERE stonadstype = :stonadstype
            """.trimMargin(),
            mapOf("stonadstype" to Stønadstype.LÆREMIDLER.name),
        ) { rs: ResultSet, _: Int ->
            BehandlingInfo(
                FagsakId.fromString(rs.getString("fagsak_id")),
                BehandlingId.fromString(rs.getString("behandling_id")),
            )
        }
}

data class BehandlingInfo(
    val fagsakId: FagsakId,
    val behandlingId: BehandlingId,
)
