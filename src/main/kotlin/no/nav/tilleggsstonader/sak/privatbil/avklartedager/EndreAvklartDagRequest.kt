package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import java.time.LocalDate

data class EndreAvklartDagRequest(
    val dato: LocalDate,
    val godkjentGjennomførtKjøring: GodkjentGjennomførtKjøring,
    val parkeringsutgift: Int?,
    val begrunnelse: String? = null,
) {
    init {
        require(godkjentGjennomførtKjøring != GodkjentGjennomførtKjøring.IKKE_VURDERT) { "Må vurdere om kjøring er godkjent eller ikke" }
    }
}
