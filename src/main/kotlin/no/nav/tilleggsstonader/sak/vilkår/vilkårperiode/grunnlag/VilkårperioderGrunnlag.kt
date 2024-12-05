package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype as EnsligForsørgerStønadstypeKontrakter

data class VilkårperioderGrunnlag(
    val aktivitet: GrunnlagAktivitet,
    val ytelse: GrunnlagYtelse,
    val hentetInformasjon: HentetInformasjon,
)

@Table("vilkarperioder_grunnlag")
data class VilkårperioderGrunnlagDomain(
    @Id
    val behandlingId: BehandlingId,
    val grunnlag: VilkårperioderGrunnlag,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class GrunnlagAktivitet(
    val aktiviteter: List<RegisterAktivitet>,
)

// TODO hvordan håndtere de som allerede finnes i basen?

data class GrunnlagYtelse(
    val perioder: List<PeriodeGrunnlagYtelse>,
    val hentedeYtelser: List<HentetYtelse>,
) {
    data class HentetYtelse(
        val type: TypeYtelsePeriode,
        val status: StatusHentetInformasjon
    )

    enum class StatusHentetInformasjon {
        OK,
        FEILET,
        ;
        companion object {
            fun from(status: no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon) = when (status) {
                no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon.OK -> OK
                no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon.FEILET -> FEILET
            }
        }
    }
}

/**
 * Kopi av [no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto]
 */
data class RegisterAktivitet(
    val id: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: String,
    val typeNavn: String,
    val status: StatusAktivitet?,
    val statusArena: String?,
    val antallDagerPerUke: Int?,
    val prosentDeltakelse: BigDecimal?,
    val erStønadsberettiget: Boolean?,
    val erUtdanning: Boolean?,
    val arrangør: String?,
    val kilde: Kilde,
)

data class PeriodeGrunnlagYtelse(
    val type: TypeYtelsePeriode,
    val fom: LocalDate,
    val tom: LocalDate?,
    val ensligForsørgerStønadstype: EnsligForsørgerStønadstype? = null,
)

data class HentetInformasjon(
    val fom: LocalDate,
    val tom: LocalDate,
    val tidspunktHentet: LocalDateTime,
)

enum class EnsligForsørgerStønadstype {
    OVERGANGSSTØNAD,
    SKOLEPENGER,
}

fun EnsligForsørgerStønadstypeKontrakter.tilDomenetype(): EnsligForsørgerStønadstype {
    return when (this) {
        EnsligForsørgerStønadstypeKontrakter.OVERGANGSSTØNAD -> EnsligForsørgerStønadstype.OVERGANGSSTØNAD
        EnsligForsørgerStønadstypeKontrakter.SKOLEPENGER -> EnsligForsørgerStønadstype.SKOLEPENGER
        EnsligForsørgerStønadstypeKontrakter.BARNETILSYN -> error("BARNETILSYN skal ikke brukes i vilkårperioder grunnlag")
    }
}
