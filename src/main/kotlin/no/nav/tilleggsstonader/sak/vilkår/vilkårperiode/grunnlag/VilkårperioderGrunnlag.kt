package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse.YtelseSubtype.AAP_FERDIG_AVKLART
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype as EnsligForsørgerStønadstypeKontrakter
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode as YtelsePeriodeKontrakter

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

data class GrunnlagYtelse(
    val perioder: List<PeriodeGrunnlagYtelse>,
)

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
    val subtype: YtelseSubtype? = null,
) {
    init {
        feilHvis(subtype != null && subtype.gyldigSammenMed != type) {
            "Ugyldig kombinasjon av type=$type og subtype=$subtype"
        }
    }

    /**
     * [YtelseSubtype] brukes for ev ekstrainformasjon om en periode
     *
     * @param AAP_FERDIG_AVKLART brukes når AAP-periode har status ferdig avklart.
     * Den skal ikke være valgbart då det er en AAP-periode som ikke gir rett på stønad
     */
    enum class YtelseSubtype(
        val gyldigSammenMed: TypeYtelsePeriode,
    ) {
        AAP_FERDIG_AVKLART(TypeYtelsePeriode.AAP),
        OVERGANGSSTØNAD(TypeYtelsePeriode.ENSLIG_FORSØRGER),
        SKOLEPENGER(TypeYtelsePeriode.ENSLIG_FORSØRGER),
    }
}

data class HentetInformasjon(
    val fom: LocalDate,
    val tom: LocalDate,
    val tidspunktHentet: LocalDateTime,
)

fun YtelsePeriodeKontrakter.tilYtelseSubtype(): PeriodeGrunnlagYtelse.YtelseSubtype? =
    when (this.type) {
        TypeYtelsePeriode.ENSLIG_FORSØRGER -> {
            when (this.ensligForsørgerStønadstype) {
                EnsligForsørgerStønadstypeKontrakter.OVERGANGSSTØNAD -> PeriodeGrunnlagYtelse.YtelseSubtype.OVERGANGSSTØNAD
                EnsligForsørgerStønadstypeKontrakter.SKOLEPENGER -> PeriodeGrunnlagYtelse.YtelseSubtype.SKOLEPENGER
                else -> error("Skal ikke mappe grunnlag for ${this.ensligForsørgerStønadstype}")
            }
        }

        TypeYtelsePeriode.AAP -> {
            when (this.aapErFerdigAvklart) {
                true -> PeriodeGrunnlagYtelse.YtelseSubtype.AAP_FERDIG_AVKLART
                else -> null
            }
        }

        else -> null
    }
