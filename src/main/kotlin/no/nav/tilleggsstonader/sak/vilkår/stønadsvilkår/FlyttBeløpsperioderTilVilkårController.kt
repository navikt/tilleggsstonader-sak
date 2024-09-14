package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
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
            .filter { it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL }

        val behandlingerUnderArbeid = getBehandlingerUnderArbeid(oppfylteVilkåtUtenFomOgTom)

        oppfylteVilkåtUtenFomOgTom
            .groupBy { it.barnId }
            .mapNotNull {
                if (it.value.size == 1) {
                    it.value.single()
                } else {
                    logger.info("Vilkår for barn=${it.key} har flere vilkår, ignorerer")
                    null
                }
            }.forEach { vilkår ->
                val behandling = behandlingerUnderArbeid[vilkår.behandlingId]
                if (behandling != null) {
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
        val forrigeBarnId = finnForrigeBarnId(behandling, vilkår.barnId!!)

        if (forrigeBarnId == null) {
            logger.info("Finner ikke match til barn=${vilkår.barnId}")
        } else {
            val forrigeVilkår = vilkårPåBarnId.getValue(forrigeBarnId).let {
                if (it.size == 1) {
                    it.single()
                } else {
                    logger.info("Vilkår=${vilkår.id} har flere vilkår i forrige behandling, mapper ikke")
                    return
                }
            }
            if (forrigeVilkår.resultat != Vilkårsresultat.OPPFYLT) {
                logger.info("Vilkår=${vilkår.id} forrige vilkår har ikke resultat oppfylt")
                return
            }
            if (forrigeVilkår.fom == null || forrigeVilkår.tom == null || forrigeVilkår.utgift == null) {
                logger.info("Vilkår=${vilkår.id} med forrigeVilkår=${forrigeVilkår.id} har ikke perioder")
            } else {
                oppdaterVilkår(vilkår, forrigeVilkår, dryRun)
            }
        }
    }

    private fun getBehandlingerUnderArbeid(oppfylteVilkåtUtenFomOgTom: List<Vilkår>) =
        behandlingRepository.findAllById(oppfylteVilkåtUtenFomOgTom.map { it.behandlingId }.toSet())
            .filter { it.status in statusUnderArbeid }
            .filter { it.steg in setOf(StegType.INNGANGSVILKÅR, StegType.VILKÅR) }
            .filter { it.forrigeBehandlingId != null }
            .associateBy { it.id }

    private fun oppdaterVilkår(vilkår: Vilkår, forrigeVilkår: Vilkår, dryRun: String) {
        logger.info("Oppdaterer vilkår=${vilkår.id} med data fra ${forrigeVilkår.id}")
        if (dryRun == "false") {
            jdbcTemplate.update(
                "UPDATE vilkar SET fom=:fom, tom=:tom, utgift=:utgift, delvilkar=:delvilkar::json," +
                    "resultat=:resultat, opphavsvilkaar_behandling_id=:opphavsvilkaar_behandling_id," +
                    "opphavsvilkaar_vurderingstidspunkt=:opphavsvilkaar_vurderingstidspunkt" +
                    " WHERE id=:id",
                mapOf(
                    "id" to vilkår.id,
                    "fom" to forrigeVilkår.fom,
                    "tom" to forrigeVilkår.tom,
                    "utgift" to forrigeVilkår.utgift,
                    "delvilkar" to objectMapper.writeValueAsString(forrigeVilkår.delvilkårsett),
                    "resultat" to forrigeVilkår.resultat.name,
                    "opphavsvilkaar_behandling_id" to forrigeVilkår.behandlingId,
                    "opphavsvilkaar_vurderingstidspunkt" to forrigeVilkår.sporbar.endret.endretTid,
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
