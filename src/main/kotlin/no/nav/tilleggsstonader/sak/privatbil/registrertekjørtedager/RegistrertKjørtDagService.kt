package no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RegistrertKjørtDagService(
    private val registrertKjørtUkeRepository: RegistrertKjørtUkeRepository,
) {
    fun hentForBehandling(behandlingId: BehandlingId): List<RegistrertKjørtUke> =
        registrertKjørtUkeRepository.findByBehandlingId(behandlingId).map { it }

    fun lagreUke(
        behandlingId: BehandlingId,
        request: RegistrertKjørtUkePostRequest,
    ): RegistrertKjørtUke {
        val uke =
            RegistrertKjørtUke(
                behandlingId = behandlingId,
                reiseId = request.reiseId,
                begrunnelse = request.begrunnelse,
                dager = request.dager.tilDomene(),
            )
        return registrertKjørtUkeRepository.insert(uke)
    }

    fun oppdaterUke(
        behandlingId: BehandlingId,
        ukeId: UUID,
        request: RegistrertKjørtUkePutRequest,
    ): RegistrertKjørtUke {
        val eksisterende = registrertKjørtUkeRepository.findByIdOrThrow(ukeId)
        brukerfeilHvis(eksisterende.behandlingId != behandlingId) {
            "Uke $ukeId tilhører ikke behandling $behandlingId"
        }
        return registrertKjørtUkeRepository
            .update(
                eksisterende.copy(
                    begrunnelse = request.begrunnelse,
                    dager = request.dager.tilDomene(),
                ),
            )
    }

    private fun List<RegistrertKjørtDagRequest>.tilDomene(): Set<RegistrertKjørtDag> =
        map { dag ->
            RegistrertKjørtDag(
                dato = dag.dato,
                harKjørt = dag.harKjørt,
                parkeringsutgift = dag.parkeringsutgift,
            )
        }.toSet()
}
