package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingForOppfølgingDto(
    val behandling: BehandlingInformasjon,
    val stønadsperioderForKontroll: List<StønadsperiodeForKontroll>,
    val registerAktiviteter: List<RegisterAktivitetDto>,
)

data class StønadsperiodeForKontroll(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val endringAktivitet: Set<ÅrsakKontroll>,
    val endringMålgruppe: Set<ÅrsakKontroll>,
) {
    fun trengerKontroll(): Boolean = (endringAktivitet + endringMålgruppe).any { it.trengerKontroll }
}

enum class ÅrsakKontroll(
    val trengerKontroll: Boolean = true,
) {
    SKAL_IKKE_KONTROLLERES(trengerKontroll = false),
    INGEN_ENDRING(trengerKontroll = false),

    INGEN_TREFF,
    FOM_ENDRET,
    TOM_ENDRET,
    TREFF_MEN_FEIL_TYPE,
}

data class BehandlingInformasjon(
    val behandlingId: BehandlingId,
    val fagsakId: FagsakId,
    val eksternFagsakId: Long,
    val stønadstype: Stønadstype,
    val vedtakstidspunkt: LocalDateTime,
)

data class RegisterAktivitetDto(
    val id: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val typeNavn: String,
    val status: StatusAktivitet?,
    val erUtdanning: Boolean?,
)
