package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder

@RestController
@RequestMapping(path = ["/api/grunnlag/patch"])
@ProtectedWithClaims(issuer = "azuread")
class GrunnlagPatchController(
    private val behandlingRepository: BehandlingRepository,
    private val grunnlagsdataRepository: GrunnlagsdataRepository,
    private val arenaService: ArenaService,
) {

    @PostMapping
    fun patchGrunnlag() {
        if (RequestContextHolder.getRequestAttributes() != null) {
            SpringTokenValidationContextHolder().setTokenValidationContext(null)
        }
        behandlingRepository.findAll()
            .filter { setOf(BehandlingStatus.UTREDES, BehandlingStatus.OPPRETTET, BehandlingStatus.SATT_PÅ_VENT).contains(it.status) }
            .forEach {
                val grunnlag = grunnlagsdataRepository.findByIdOrNull(it.id)

                if (grunnlag != null && grunnlag.grunnlag.arena == null) {
                    val saksbehandling = behandlingRepository.finnSaksbehandling(it.id)
                    val statusArena = arenaService.hentStatus(saksbehandling.ident, saksbehandling.stønadstype)
                    val grunnlagArena = GrunnlagArenaMapper.mapFaktaArena(statusArena)

                    val oppdatertGrunnlag = grunnlag.grunnlag.copy(arena = grunnlagArena)
                    grunnlagsdataRepository.update(grunnlag.copy(grunnlag = oppdatertGrunnlag))
                }
            }
    }
}
