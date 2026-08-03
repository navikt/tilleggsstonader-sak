package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

data class KjørelisteOversiktDto(
    val tilgjengeligeReiser: List<ManuellRegistreringReiseDto>,
    val kjørelisterLagretIBehandling: List<ManueltInnsendtKjørelisteDto>,
)

data class ManueltInnsendtKjørelisteDto(
    val id: KjørelisteId,
    val reiseFom: LocalDate,
    val reiseTom: LocalDate,
    val aktivitetsadresse: String?,
    val begrunnelse: String?,
    val journalpostId: String?,
    val innsendteUker: List<ManueltInnsendtKjørelisteUkeDto>,
)

data class ManueltInnsendtKjørelisteUkeDto(
    val ukenummer: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val dager: List<KjørelisteDag>,
)

data class ManuellRegistreringReiseDto(
    val reiseId: ReiseId,
    val aktivitetsadresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val uker: List<ManuellRegistreringUkeDto>,
)

data class ManuellRegistreringUkeDto(
    val ukenummer: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val dager: List<LocalDate>,
    val innsendtTidligere: Boolean,
)
