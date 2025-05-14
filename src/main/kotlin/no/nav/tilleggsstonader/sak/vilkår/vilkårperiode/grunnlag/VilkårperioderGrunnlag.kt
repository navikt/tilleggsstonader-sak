package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
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

/**
 * Liste med aktiviteter hentet fra Arena
 *
 * Hvis kallet for å hente aktiviteter feiler er det ønskelig å stoppe saksbehandler fra å fortsette saksbehandle.
 * Dvs man skal ikke kunne legge inn aktiviteter manuellt, då vi ønsker mest mulig kobling til en registerperiode på id
 */
data class GrunnlagAktivitet(
    val aktiviteter: List<RegisterAktivitet>,
)

/**
 * Liste med ytelser hentet fra ulike kilder nevnt i [kildeResultat]
 *
 * Dersom henting av eks AAP-perioder feilet ønsker man ikke å stoppe saksbehandler fra å saksbehandle,
 *  men at man viser at kallene til AAP feilet og at man kan hente de på nytt
 *
 * @param kildeResultat inneholder informasjon om hvilke kilder vi hentet informasjon fra
 */
data class GrunnlagYtelse(
    val perioder: List<PeriodeGrunnlagYtelse>,
    val kildeResultat: List<KildeResultatYtelse>,
) {
    data class KildeResultatYtelse(
        val type: TypeYtelsePeriode,
        val resultat: ResultatKilde,
    )
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
) {
    constructor(aktivitet: no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto) : this(
        id = aktivitet.id,
        fom = aktivitet.fom,
        tom = aktivitet.tom,
        type = aktivitet.type,
        typeNavn = aktivitet.typeNavn,
        status = aktivitet.status,
        statusArena = aktivitet.statusArena,
        antallDagerPerUke = aktivitet.antallDagerPerUke,
        prosentDeltakelse = aktivitet.prosentDeltakelse,
        erStønadsberettiget = aktivitet.erStønadsberettiget,
        erUtdanning = aktivitet.erUtdanning,
        arrangør = aktivitet.arrangør,
        kilde = aktivitet.kilde,
    )
}

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

/**
 * Informasjon som ble brukt når aktivitet og ytelser ble hentet
 */
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
                true -> AAP_FERDIG_AVKLART
                else -> null
            }
        }

        else -> null
    }
