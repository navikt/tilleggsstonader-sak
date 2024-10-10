package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate

@RestController
@RequestMapping("/api/vedtak/admin")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KopierTidligereBeregningsperioderController(
    private val vedtakRepository: TilsynBarnVedtakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    @GetMapping
    fun finnBehandlingerSomTrengerOppdatering(): List<BehandlingId> {
        utførEndringSomSystem()

        return analyserVedtak().map { it.vedtak.behandlingId }
    }

    @PostMapping("oppdater")
    @Transactional
    fun oppdaterBeregningsresultat() {
        utførEndringSomSystem()

        analyserVedtak().forEach {
            if (it.trengerOppdatering) {
                logger.warn("Oppdaterer vedtak for behandling=${it.vedtak.behandlingId} med nytt beregningsresultat")

                jdbcTemplate.update(
                    "UPDATE vedtak_tilsyn_barn SET beregningsresultat=:beregningsresultat::json WHERE behandling_id=:behandlingId",
                    mapOf(
                        "beregningsresultat" to objectMapper.writeValueAsString(it.nyttBeregningsresultat),
                        "behandlingId" to it.vedtak.behandlingId.id,
                    ),
                )
            }
        }
    }

    private fun analyserVedtak(): List<Oppdateringsinformasjon> {
        val behandlinger = behandlingRepository.findAll()
            .mapNotNull { behandling -> behandling.revurderFra?.let { behandlingRepository.finnSaksbehandling(behandling.id) } }
            .associateBy { it.id }

        val alleVedtak = vedtakRepository.findAllById(behandlinger.keys)
        return alleVedtak.mapNotNull { vedtak ->
            val behandlingId = vedtak.behandlingId

            if (vedtak.type != TypeVedtak.INNVILGELSE) {
                logger.info("Ignorerer vedtak for behandling=$behandlingId då det er et avslag")
                return@mapNotNull null
            }
            val behandling = behandlinger.getValue(behandlingId)
            val beregningsresultatTilsynBarn = tilsynBarnBeregningService.beregn(behandling)

            var trengerOppdatering = beregningsresultatTilsynBarn != vedtak.beregningsresultat
            logger.info("Vedtak for behandling=$behandlingId trengerOppdatering=$trengerOppdatering")

            val eksisterendeAndeler = finnAndeler(behandling)
            if (eksisterendeAndeler != tilAndeler(beregningsresultatTilsynBarn)) {
                logger.warn("Genererte andeler er ikke like for behandling=$behandlingId")
                trengerOppdatering = false
            } else {
                logger.info("Andeler er like for behandling=$behandlingId")
            }
            Oppdateringsinformasjon(vedtak, beregningsresultatTilsynBarn, trengerOppdatering)
        }
    }

    private data class Oppdateringsinformasjon(
        val vedtak: VedtakTilsynBarn,
        val nyttBeregningsresultat: BeregningsresultatTilsynBarn,
        val trengerOppdatering: Boolean,
    )

    /**
     * Kopi av [TilsynBarnBeregnYtelseSteg.lagreAndeler] for å opprette andeler
     * Med endring av at man oppretter [ForenkletAndel] for å enkelt kunne diffe
     */
    private fun tilAndeler(beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn): List<ForenkletAndel> {
        return beregningsresultatTilsynBarn.perioder.flatMap {
            it.beløpsperioder.map { beløpsperiode ->
                val satstype = Satstype.DAG
                val ukedag = beløpsperiode.dato.dayOfWeek
                feilHvis(ukedag == DayOfWeek.SATURDAY || ukedag == DayOfWeek.SUNDAY) {
                    "Skal ikke opprette perioder som begynner på en helgdag for satstype=$satstype"
                }
                ForenkletAndel(
                    fom = beløpsperiode.dato,
                    tom = beløpsperiode.dato,
                    beløp = beløpsperiode.beløp,
                    satstype = satstype,
                    type = beløpsperiode.målgruppe.tilTypeAndel(),
                )
            }
        }
            .sortedBy { it.fom }
    }

    private fun finnAndeler(behandling: Saksbehandling) =
        (
            tilkjentYtelseRepository.findByBehandlingId(behandling.id)?.andelerTilkjentYtelse
                ?.sortedBy { it.fom }
                ?.map {
                    ForenkletAndel(
                        fom = it.fom,
                        tom = it.tom,
                        beløp = it.beløp,
                        satstype = it.satstype,
                        type = it.type,
                    )
                }
                ?: error("Kunne ikke finne tilkjentYtelse for behandling=${behandling.id}")
            )

    data class ForenkletAndel(
        val fom: LocalDate,
        val tom: LocalDate,
        val beløp: Int,
        val satstype: Satstype,
        val type: TypeAndel,
    )

    private fun MålgruppeType.tilTypeAndel(): TypeAndel {
        return when (this) {
            MålgruppeType.AAP, MålgruppeType.UFØRETRYGD, MålgruppeType.NEDSATT_ARBEIDSEVNE -> TypeAndel.TILSYN_BARN_AAP
            MålgruppeType.OVERGANGSSTØNAD -> TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER
            MålgruppeType.OMSTILLINGSSTØNAD -> TypeAndel.TILSYN_BARN_ETTERLATTE
            else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
        }
    }
}
