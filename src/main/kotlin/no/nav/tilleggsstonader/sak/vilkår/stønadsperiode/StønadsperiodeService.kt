package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StønadsperiodeService(
    private val behandlingService: BehandlingService,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun hentStønadsperioder(behandlingId: UUID): List<StønadsperiodeDto> {
        return stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
    }

    @Transactional
    fun lagreStønadsperioder(behandlingId: UUID, stønadsperioder: List<StønadsperiodeDto>): List<StønadsperiodeDto> {
        feilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke lagre stønadsperioder når behandlingen er låst"
        }
        validerStønadsperioder(behandlingId, stønadsperioder)

        val tidligereStønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId)

        slettStønadsperioder(tidligereStønadsperioder, stønadsperioder)
        val oppdaterteStønadsperioder = oppdaterEksisterendeStønadsperioder(tidligereStønadsperioder, stønadsperioder)
        val nyeStønadsperioder = leggTilNyeStønadsperioder(behandlingId, stønadsperioder, tidligereStønadsperioder)
        return (nyeStønadsperioder + oppdaterteStønadsperioder).tilSortertDto()
    }

    private fun slettStønadsperioder(
        tidligereStønadsperioder: List<Stønadsperiode>,
        stønadsperioder: List<StønadsperiodeDto>,
    ) {
        val gjenståendePerioder = stønadsperioder.mapNotNull { it.id }.toSet()
        val perioderTilSletting = tidligereStønadsperioder.filterNot { gjenståendePerioder.contains(it.id) }
        stønadsperiodeRepository.deleteAllById(perioderTilSletting.map { it.id })
    }

    private fun oppdaterEksisterendeStønadsperioder(
        tidligereStønadsperioder: List<Stønadsperiode>,
        stønadsperioder: List<StønadsperiodeDto>,
    ): List<Stønadsperiode> {
        val stønadsperioderPåId =
            stønadsperioder.mapNotNull { periode -> periode.id?.let { id -> id to periode } }.toMap()
        val perioderTilOppdatering = tidligereStønadsperioder.mapNotNull { tidligereStønadsperiode ->
            stønadsperioderPåId[tidligereStønadsperiode.id]?.let {
                tidligereStønadsperiode.copy(
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = it.målgruppe,
                    aktivitet = it.aktivitet,
                )
            }
        }
        return stønadsperiodeRepository.updateAll(perioderTilOppdatering)
    }

    private fun leggTilNyeStønadsperioder(
        behandlingId: UUID,
        stønadsperioder: List<StønadsperiodeDto>,
        tidligereStønadsperiode: List<Stønadsperiode>,
    ): List<Stønadsperiode> {
        val tidligereStønadsperiodeIder = tidligereStønadsperiode.map { it.id }.toSet()
        val nyeStønadsperioder = stønadsperioder.filterNot { tidligereStønadsperiodeIder.contains(it.id) }.map {
            feilHvis(it.id != null) {
                "Kan ikke oppdatere stønadsperiode=${it.id} som ikke finnes fra før"
            }
            Stønadsperiode(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                fom = it.fom,
                tom = it.tom,
                målgruppe = it.målgruppe,
                aktivitet = it.aktivitet,
            )
        }

        return stønadsperiodeRepository.insertAll(nyeStønadsperioder)
    }

    fun validerStønadsperioder(behandlingId: UUID, stønadsperioder: List<StønadsperiodeDto>) {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        StønadsperiodeValideringUtil.validerStønadsperioder(stønadsperioder, vilkårperioder)
    }
}
