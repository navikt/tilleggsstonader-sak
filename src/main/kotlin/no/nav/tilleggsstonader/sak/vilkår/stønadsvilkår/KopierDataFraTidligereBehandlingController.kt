package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
) {

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
        @PathVariable behandlingId: UUID,
        @RequestParam(required = false) forrigeBehandlingId: UUID? = null,
    ) {
        utførEndringSomSystem()

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerHarLikFagsak(forrigeBehandlingId, behandling)
        validerRiktigStatus(behandling)

        val gjenbrukFraId = forrigeBehandlingId ?: behandling.forrigeBehandlingId!!
        val perioder = vilkårperiodeService.hentVilkårperioderDto(behandling.id)
        val forrigePerioder = vilkårperiodeService.hentVilkårperioderDto(gjenbrukFraId)

        brukerfeilHvis(perioder.målgrupper.any { v -> forrigePerioder.målgrupper.any { it.overlapper(v) } }) {
            "Har målgrupper som overlapper"
        }

        brukerfeilHvis(perioder.aktiviteter.any { v -> forrigePerioder.aktiviteter.any { it.overlapper(v) } }) {
            "Har aktiviteter som overlapper"
        }
        vilkårperiodeService.gjenbrukVilkårperioder(forrigeBehandlingId = gjenbrukFraId, nyBehandlingId = behandlingId)
    }

    @PostMapping("{behandlingId}/stonadsperioder")
    @Transactional
    fun stønadsperioder(
        @PathVariable behandlingId: UUID,
        @RequestParam(required = false) forrigeBehandlingId: UUID? = null,
    ) {
        utførEndringSomSystem()

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerHarLikFagsak(forrigeBehandlingId, behandling)
        validerRiktigStatus(behandling)

        val gjenbrukFra = forrigeBehandlingId ?: behandling.forrigeBehandlingId!!
        val perioder = stønadsperiodeService.hentStønadsperioder(behandling.id)
        val forrigePerioder = stønadsperiodeService.hentStønadsperioder(gjenbrukFra)

        brukerfeilHvis(perioder.any { v -> forrigePerioder.any { it.overlapper(v) } }) {
            "Har stønadsperioder som overlapper"
        }
        stønadsperiodeService.gjenbrukStønadsperioder(forrigeBehandlingId = gjenbrukFra, nyBehandlingId = behandlingId)
    }

    private fun validerRiktigStatus(behandling: Behandling) {
        brukerfeilHvisIkke(behandling.status in statusUnderArbeid) {
            "Behandling har feil status=${behandling.status}"
        }
    }

    private fun validerHarLikFagsak(
        forrigeBehandling: UUID?,
        behandling: Behandling,
    ) {
        forrigeBehandling?.let {
            brukerfeilHvisIkke(behandling.fagsakId == behandlingRepository.findByIdOrThrow(it).fagsakId) {
                "Behandlinger har ikke den samme fagsaken"
            }
        }
    }
}
