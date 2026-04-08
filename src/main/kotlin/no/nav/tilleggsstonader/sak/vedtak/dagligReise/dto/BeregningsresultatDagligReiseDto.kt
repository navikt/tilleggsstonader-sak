package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.BeregningDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dto.BeregningsplanDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningDagligReiseDto(
    val beregningsresultat: BeregningsresultatDagligReiseDto,
    val rammevedtakPrivatBil: RammevedtakPrivatBilDto?,
)

data class BeregningsresultatDagligReiseDto(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
    val privatBil: BeregningsresultatPrivatBilDto?,
    val beregningsplan: BeregningsplanDto,
    val tidligsteEndring: LocalDate? = beregningsplan.fraDato,
)

data class BeregningsresultatOffentligTransportDto(
    val reiser: List<BeregningsresultatForReiseDto>,
)

data class BeregningsresultatForReiseDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val perioder: List<BeregningsresultatForPeriodeDto>,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val antallReisedagerPerUke: Int,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
    val antallReisedager: Int,
    val fraTidligereVedtak: Boolean,
    val brukersNavKontor: String?,
) : Periode<LocalDate>

data class BeregningsresultatPrivatBilDto(
    val reiser: List<BeregningsresultatForReisePrivatBilDto>,
)

data class BeregningsresultatForReisePrivatBilDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val reisedagerPerUke: Int?,
    val perioder: List<BeregningsresultatForPeriodePrivatBilDto>,
)

data class BeregningsresultatForPeriodePrivatBilDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val grunnlag: BeregningsresultatForReisePrivatBilGrunnlagDto,
    val stønadsbeløp: BigDecimal,
    val brukersNavKontor: String?,
)

data class BeregningsresultatForReisePrivatBilGrunnlagDto(
    val dager: List<BeregningsresultatForReisePrivatBilDagDto>,
    val dagsatsUtenParkering: BigDecimal,
)

data class BeregningsresultatForReisePrivatBilDagDto(
    val dato: LocalDate,
    val parkeringskostnad: Int,
    val stønadsbeløpForDag: BigDecimal,
)

fun BeregningDagligReise.tilDto(
    beregningsplan: Beregningsplan,
    vilkår: List<VilkårDagligReise>,
): BeregningDagligReiseDto =
    BeregningDagligReiseDto(
        beregningsresultat = beregningsresultatDagligReise.tilDto(beregningsplan, vilkår),
        rammevedtakPrivatBil = rammevedtakPrivatBil?.tilDto(),
    )

fun BeregningsresultatDagligReise.tilDto(
    beregningsplan: Beregningsplan,
    vilkår: List<VilkårDagligReise>,
): BeregningsresultatDagligReiseDto =
    BeregningsresultatDagligReiseDto(
        offentligTransport = offentligTransport?.tilDto(vilkår),
        privatBil = privatBil?.tilDto(vilkår),
        beregningsplan = beregningsplan.tilDto(),
    )

fun BeregningsresultatOffentligTransport.tilDto(vilkår: List<VilkårDagligReise>): BeregningsresultatOffentligTransportDto =
    BeregningsresultatOffentligTransportDto(
        reiser = reiser.map { it.tilDto(vilkår) },
    )

fun BeregningsresultatForReise.tilDto(vilkår: List<VilkårDagligReise>): BeregningsresultatForReiseDto =
    BeregningsresultatForReiseDto(
        reiseId = reiseId,
        adresse = vilkår.firstOrNull { it.fakta.reiseId == reiseId }?.fakta?.adresse,
        perioder = perioder.map { it.tilDto() },
    )

fun BeregningsresultatForPeriode.tilDto(): BeregningsresultatForPeriodeDto =
    BeregningsresultatForPeriodeDto(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        prisEnkeltbillett = grunnlag.prisEnkeltbillett,
        prisSyvdagersbillett = grunnlag.prisSyvdagersbillett,
        pris30dagersbillett = grunnlag.pris30dagersbillett,
        antallReisedagerPerUke = grunnlag.antallReisedagerPerUke,
        beløp = beløp,
        billettdetaljer = billettdetaljer,
        antallReisedager = grunnlag.antallReisedager,
        fraTidligereVedtak = fraTidligereVedtak,
        brukersNavKontor = grunnlag.brukersNavKontor,
    )

fun BeregningsresultatPrivatBil.tilDto(vilkår: List<VilkårDagligReise>): BeregningsresultatPrivatBilDto =
    BeregningsresultatPrivatBilDto(
        reiser = reiser.map { it.tilDto(vilkår) },
    )

fun BeregningsresultatForReisePrivatBil.tilDto(vilkår: List<VilkårDagligReise>): BeregningsresultatForReisePrivatBilDto {
    val vilkårForReise =
        vilkår.filter { it.fakta.type == TypeDagligReise.PRIVAT_BIL }.firstOrNull { it.fakta.reiseId == reiseId }
    val vilkårFakta = vilkårForReise?.fakta?.mapTilVilkårFakta() as? FaktaDagligReisePrivatBil

    return BeregningsresultatForReisePrivatBilDto(
        reiseId = reiseId,
        adresse = vilkårForReise?.fakta?.adresse,
        // TODO Mappe om denne til delperioder
        reisedagerPerUke = vilkårFakta?.faktaDelperioder?.single()?.reisedagerPerUke,
        perioder = perioder.map { it.tilDto() },
    )
}

fun BeregningsresultatForReisePrivatBilPeriode.tilDto(): BeregningsresultatForPeriodePrivatBilDto =
    BeregningsresultatForPeriodePrivatBilDto(
        fom = fom,
        tom = tom,
        grunnlag =
            BeregningsresultatForReisePrivatBilGrunnlagDto(
                dager =
                    grunnlag.dager.map {
                        BeregningsresultatForReisePrivatBilDagDto(
                            dato = it.dato,
                            parkeringskostnad = it.parkeringskostnad,
                            stønadsbeløpForDag = it.stønadsbeløpForDag,
                        )
                    },
                dagsatsUtenParkering = grunnlag.dagsatsUtenParkering,
            ),
        stønadsbeløp = stønadsbeløp,
        brukersNavKontor = brukersNavKontor,
    )
