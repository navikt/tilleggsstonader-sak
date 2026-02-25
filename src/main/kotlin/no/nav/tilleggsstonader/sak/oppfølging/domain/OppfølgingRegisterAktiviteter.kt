package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import org.slf4j.LoggerFactory

class OppfølgingRegisterAktiviteter(
    private val aktiviteter: List<AktivitetArenaDto>,
) {
    private val aktivitetPåId: Map<String, AktivitetArenaDto> by lazy {
        aktiviteter.associateBy { it.id }
    }

    val alleAktiviteter: List<DatoperiodeNullableTom> by lazy {
        aktiviteter.mergeSammenhengende()
    }
    val tiltak: List<DatoperiodeNullableTom> by lazy {
        aktiviteter.filterNot(::tiltakErUtdanning).mergeSammenhengende()
    }
    val utdanningstiltak: List<DatoperiodeNullableTom> by lazy {
        aktiviteter.filter(::tiltakErUtdanning).mergeSammenhengende()
    }

    fun forId(id: String?) = id?.let { aktivitetPåId[it] }

    /**
     * Slår sammen sammenhengende aktiviteter.
     * Aktiviteter slås sammen hvis de er sammenhengende og fom og tom eksisterer.
     * Unntaket er for den siste aktiviteten i en sammenhengende gruppe: den kan ha tom == null.
     * En aktivitet med tom == null midt i en gruppe bryter kjeden.
     * Aktiviteter uten fom filtreres vekk.
     */
    private fun List<AktivitetArenaDto>.mergeSammenhengende(): List<DatoperiodeNullableTom> =
        this
            .mapNotNull { mapTilPeriode(it) }
            .mergeSammenhengende()

    private fun mapTilPeriode(aktivitet: AktivitetArenaDto): DatoperiodeNullableTom? {
        if (aktivitet.fom == null) {
            logger.warn("Aktivitet med id=${aktivitet.id} mangler fom dato")
            return null
        }
        return DatoperiodeNullableTom(aktivitet.fom!!, aktivitet.tom)
    }

    private fun tiltakErUtdanning(it: AktivitetArenaDto) = it.erUtdanning ?: false

    companion object {
        private val logger = LoggerFactory.getLogger(OppfølgingRegisterAktiviteter::class.java)
    }
}
