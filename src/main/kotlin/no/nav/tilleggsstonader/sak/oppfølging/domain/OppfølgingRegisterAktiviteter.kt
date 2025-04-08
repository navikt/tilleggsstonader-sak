package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import org.slf4j.LoggerFactory

class OppfølgingRegisterAktiviteter(
    private val aktiviteter: List<AktivitetArenaDto>,
) {
    private val aktivitetPåId: Map<String, AktivitetArenaDto> by lazy {
        aktiviteter.associateBy { it.id }
    }

    val alleAktiviteter: List<Datoperiode> by lazy {
        aktiviteter.mergeSammenhengende()
    }
    val tiltak: List<Datoperiode> by lazy {
        aktiviteter.filterNot(::tiltakErUtdanning).mergeSammenhengende()
    }
    val utdanningstiltak: List<Datoperiode> by lazy {
        aktiviteter.filter(::tiltakErUtdanning).mergeSammenhengende()
    }

    fun forId(id: String?) = id?.let { aktivitetPåId[it] }

    private fun List<AktivitetArenaDto>.mergeSammenhengende() =
        this
            .mapNotNull { mapTilPeriode(it) }
            .mergeSammenhengende()

    private fun mapTilPeriode(aktivitet: AktivitetArenaDto): Datoperiode? {
        if (aktivitet.fom == null || aktivitet.tom == null) {
            logger.warn("Aktivitet med id=${aktivitet.id} mangler fom eller tom dato: ${aktivitet.fom} - ${aktivitet.tom}")
            return null
        }
        return Datoperiode(aktivitet.fom!!, aktivitet.tom!!)
    }

    private fun tiltakErUtdanning(it: AktivitetArenaDto) = it.erUtdanning ?: false

    companion object {
        private val logger = LoggerFactory.getLogger(OppfølgingRegisterAktiviteter::class.java)
    }
}
