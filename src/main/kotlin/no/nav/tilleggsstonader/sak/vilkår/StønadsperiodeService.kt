package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.tilSortertDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StønadsperiodeService(
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårService: VilkårService,
) {
    fun hentStønadsperioder(behandlingId: UUID): List<StønadsperiodeDto> {
        return stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
    }

    fun lagreStønadsperioder(behandlingId: UUID, stønadsperioder: List<StønadsperiodeDto>): List<StønadsperiodeDto> {
        // TODO valider behandling er ikke låst
        validerStønadsperioder(behandlingId, stønadsperioder)
        stønadsperiodeRepository.deleteAll() // TODO skal vi finne og oppdatere de som har blitt oppdatert/slettet
        return stønadsperiodeRepository.insertAll(
            stønadsperioder.map {
                Stønadsperiode(
                    id = it.id ?: UUID.randomUUID(),
                    behandlingId = behandlingId,
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = it.målgruppe,
                    aktivitet = it.aktivitet,
                )
            },
        ).tilSortertDto()
    }

    fun validerStønadsperioder(behandlingId: UUID, stønadsperioder: List<StønadsperiodeDto>) {
        val vilkårperioder = vilkårService.hentVilkårperioder(behandlingId)

        StønadsperiodeValideringUtil.validerStønadsperioder(stønadsperioder, vilkårperioder)
    }
}
