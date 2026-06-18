package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDagStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UtfyltDagAutomatiskVurdering
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.RammeForReiseMedPrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate
import java.util.UUID

data class ReisevurderingPrivatBilDto(
    val reiseId: ReiseId,
    val rammevedtak: RammeForReiseMedPrivatBilDto?,
    val forrigeRammevedtak: RammeForReiseMedPrivatBilDto?,
    val uker: List<UkeVurderingDto>,
)

data class UkeVurderingDto(
    val ukenummer: Int,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val endringIRammevedtakStatus: UkeEndringIRammevedtakStatus,
    val status: UkeStatus,
    val avvik: AvvikUke?,
    val behandletDato: LocalDate?,
    val kjørelisteInnsendtDato: LocalDate?, // null hvis kjøreliste ikke er mottatt
    val kjørelisteId: KjørelisteId?, // null hvis kjøreliste ikke er mottatt
    val avklartUkeId: UUID?,
    val avklartKjørtUkeStatus: AvklartKjørtUkeStatus?, // null hvis avklartKjørtUke ikke finnes
    val dager: List<DagDto>,
)

enum class UkeEndringIRammevedtakStatus {
    NY,
    SLETTET,
    UENDRET,
}

data class AvvikUke(
    val typeAvvik: TypeAvvikUke,
)

data class DagDto(
    val dato: LocalDate,
    val ukedag: String, // avklar om faktisk trenger, eller om frontend skal mappe ut fra dag
    val kjørelisteDag: KjørelisteDagDto?,
    val avklartDag: AvklartDagDto?,
)

data class KjørelisteDagDto(
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)

data class AvklartDagDto(
    val godkjentGjennomførtKjøring: GodkjentGjennomførtKjøring,
    val automatiskVurdering: UtfyltDagAutomatiskVurdering,
    val avvik: List<TypeAvvikDag>,
    val begrunnelse: String?, // må fylles ut om avvik?
    val parkeringsutgift: Int?,
    val avklartKjørtDagStatus: AvklartKjørtDagStatus,
)
