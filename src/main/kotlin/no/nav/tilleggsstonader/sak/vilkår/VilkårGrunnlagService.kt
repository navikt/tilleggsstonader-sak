package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.vilkår.dto.Fellesgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class VilkårGrunnlagService() {

    fun hentGrunnlag(
        behandlingId: UUID,
        // søknad: Søknadsverdier?,
        personident: String,
        barn: List<BehandlingBarn>,
    ): VilkårGrunnlagDto {
        return VilkårGrunnlagDto(
            fellesgrunnlag = Fellesgrunnlag("navn"),
        )
    }
}
