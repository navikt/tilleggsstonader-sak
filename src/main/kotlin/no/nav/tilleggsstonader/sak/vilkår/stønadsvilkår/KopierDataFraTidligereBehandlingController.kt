package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/vilkar/admin")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KopierDataFraTidligereBehandlingController(
    private val behandlingRepository: BehandlingRepository,
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
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

    @PostMapping("{behandlingId}/inngangsvilkar")
    @Transactional
    fun inngangsvilkår(
        @PathVariable behandlingId: BehandlingId,
    ) {
        utførEndringSomSystem()

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerRiktigStatus(behandling)

        val gjenbrukFraId = behandling.forrigeBehandlingId!!
        val perioder = vilkårperiodeService.hentVilkårperioderDto(behandling.id)
        val forrigePerioder = vilkårperiodeService.hentVilkårperioderDto(gjenbrukFraId)

        kopierVilkårperiode(forrigePerioder.målgrupper, perioder.målgrupper, behandling)
        kopierVilkårperiode(forrigePerioder.aktiviteter, perioder.aktiviteter, behandling)
    }

    private fun kopierVilkårperiode(
        forrigePerioder: List<VilkårperiodeDto>,
        nåværende: List<VilkårperiodeDto>,
        behandling: Behandling,
    ) {
        forrigePerioder
            .filter { it.resultat == ResultatVilkårperiode.OPPFYLT || it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }
            .filter { forrige -> nåværende.none { m -> forrige.overlapper(m) && m.type == forrige.type } }
            .forEach {
                logger.info("Kopirerer vilkårperiode=${it.id} til behandling=${behandling.id}")
                vilkårperiodeRepository.findByIdOrThrow(it.id)
                    .copy(
                        id = UUID.randomUUID(),
                        behandlingId = behandling.id,
                        sporbar = Sporbar(),
                    )
                    .let {
                        vilkårperiodeRepository.insert(it)
                    }
            }
    }

    @PostMapping("{behandlingId}/stonadsperioder")
    @Transactional
    fun stønadsperioder(
        @PathVariable behandlingId: BehandlingId,
    ) {
        utførEndringSomSystem()

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerRiktigStatus(behandling)

        val gjenbrukFra = behandling.forrigeBehandlingId!!
        val perioder = stønadsperiodeService.hentStønadsperioder(behandling.id)
        val forrigePerioder = stønadsperiodeService.hentStønadsperioder(gjenbrukFra)

        forrigePerioder
            .filter { forrigePeriode -> perioder.none { it.overlapper(forrigePeriode) } }
            .forEach { forrigePeriode ->
                logger.info("Kopirerer stønadsperiode=${forrigePeriode.id} til behandling=${behandling.id}")
                stønadsperiodeRepository.findByIdOrThrow(forrigePeriode.id!!)
                    .copy(
                        id = UUID.randomUUID(),
                        behandlingId = behandling.id,
                        sporbar = Sporbar(),
                    )
                    .let { stønadsperiodeRepository.insert(it) }
            }
        stønadsperiodeService.validerStønadsperioder(behandlingId)
    }

    private fun validerRiktigStatus(behandling: Behandling) {
        brukerfeilHvisIkke(behandling.status in statusUnderArbeid) {
            "Behandling har feil status=${behandling.status}"
        }
    }

    private fun validerHarLikFagsak(
        forrigeBehandling: BehandlingId?,
        behandling: Behandling,
    ) {
        forrigeBehandling?.let {
            brukerfeilHvisIkke(behandling.fagsakId == behandlingRepository.findByIdOrThrow(it).fagsakId) {
                "Behandlinger har ikke den samme fagsaken"
            }
        }
    }
}
