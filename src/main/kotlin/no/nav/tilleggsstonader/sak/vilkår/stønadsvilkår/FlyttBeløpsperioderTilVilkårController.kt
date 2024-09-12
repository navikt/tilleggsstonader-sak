package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/vilkar/admin")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FlyttBeløpsperioderTilVilkårController(
    private val vedtakTilsynBarnVedtakRepository: TilsynBarnVedtakRepository,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårRepository: VilkårRepository,
    private val barnService: BarnService,
    private val barnRepository: BarnRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    val statusUnderArbeid = setOf(
        BehandlingStatus.OPPRETTET,
        BehandlingStatus.UTREDES,
        BehandlingStatus.SATT_PÅ_VENT,
    )

    @PostMapping("under-arbeid")
    @Transactional
    fun oppdaterVilkårSomManglerReferanseTilForrigeBehandling(@RequestParam(defaultValue = "true") dryRun: String) {
        utførEndringSomSystem()

        val alleVilkår = vilkårRepository.findAll()
        val vilkårPåBarnId = alleVilkår.groupBy { it.barnId!! }
        val oppfylteVilkåtUtenFomOgTom = alleVilkår
            .filter { it.fom == null && it.tom == null && it.utgift == null }
            .filter { it.resultat == Vilkårsresultat.OPPFYLT }
            .filter { it.opphavsvilkår != null }

        val behandlingerUnderArbeid = getBehandlingerUnderArbeid(oppfylteVilkåtUtenFomOgTom)

        oppfylteVilkåtUtenFomOgTom.forEach { vilkår ->
            val behandling = behandlingerUnderArbeid[vilkår.behandlingId]
            if (behandling == null) {
                logger.info("Ignorerer vilkår på behandling=${vilkår.behandlingId} då den sannsynligvis ikke er under arbeid")
            } else {
                håndterVilkår(vilkår, behandling, vilkårPåBarnId, dryRun)
            }
        }
    }

    private fun håndterVilkår(
        vilkår: Vilkår,
        behandling: Behandling,
        vilkårPåBarnId: Map<UUID, List<Vilkår>>,
        dryRun: String,
    ) {
        require(vilkår.opphavsvilkår!!.behandlingId == behandling.forrigeBehandlingId) {
            "vilkår=${vilkår.id} opphavsbehandling=${vilkår.opphavsvilkår.behandlingId} er ikke lik ${behandling.forrigeBehandlingId}"
        }
        val forrigeBarnId = finnForrigeBarnId(behandling, vilkår.barnId!!)

        if (forrigeBarnId == null) {
            logger.info("Finner ikke match til barn=${vilkår.barnId}")
        } else {
            val forrigeVilkår = vilkårPåBarnId.getValue(forrigeBarnId).single()
            if (forrigeVilkår.fom == null || forrigeVilkår.tom == null || forrigeVilkår.utgift == null) {
                logger.info("ForrigeVilkår=${forrigeVilkår.id} har ikke perioder")
            } else {
                oppdaterVilkår(vilkår, forrigeVilkår, dryRun)
            }
        }
    }

    private fun getBehandlingerUnderArbeid(oppfylteVilkåtUtenFomOgTom: List<Vilkår>) =
        behandlingRepository.findAllById(oppfylteVilkåtUtenFomOgTom.map { it.behandlingId }.toSet())
            .filter { it.status in statusUnderArbeid }
            .associateBy { it.id }

    private fun oppdaterVilkår(vilkår: Vilkår, forrigeVilkår: Vilkår, dryRun: String) {
        logger.info("Oppdaterer vilkår=${vilkår.id} med data fra ${forrigeVilkår.id}")
        if (dryRun == "false") {
            jdbcTemplate.update(
                "UPDATE vilkar SET fom=:fom, tom=:tom, utgift=:utgift WHERE id=:id",
                mapOf(
                    "id" to vilkår.id,
                    "fom" to forrigeVilkår.fom,
                    "tom" to forrigeVilkår.tom,
                    "utgift" to forrigeVilkår.utgift,
                ),
            )
        }
    }

    private fun finnForrigeBarnId(behandling: Behandling, barnId: UUID): UUID? {
        val forrigeBehandlingId = behandling.forrigeBehandlingId!!
        val barn = barnRepository.findByIdOrThrow(barnId)
        return barnService.finnBarnPåBehandling(forrigeBehandlingId).firstOrNull { it.ident == barn.ident }?.id
    }
}
