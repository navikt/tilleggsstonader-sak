package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import java.time.LocalDate

data class BeregningsresultatDagligReiseDto(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
    val tidligsteEndring: LocalDate? = null,
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

fun BeregningsresultatDagligReise.tilDto(
    tidligsteEndring: LocalDate?,
    vilkår: List<VilkårDagligReise> = emptyList(),
): BeregningsresultatDagligReiseDto =
    BeregningsresultatDagligReiseDto(
        offentligTransport = offentligTransport?.tilDto(vilkår),
        tidligsteEndring = tidligsteEndring,
    )

fun BeregningsresultatOffentligTransport.tilDto(vilkår: List<VilkårDagligReise>): BeregningsresultatOffentligTransportDto =
    BeregningsresultatOffentligTransportDto(
        reiser =
            reiser.map {
                it.tilDto(adresse = hentTilhørendeAdresseFraFakta(vilkår, it.reiseId))
            },
    )

fun BeregningsresultatForReise.tilDto(adresse: String?): BeregningsresultatForReiseDto =
    BeregningsresultatForReiseDto(
        reiseId = reiseId,
        adresse = adresse,
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

private fun hentTilhørendeAdresseFraFakta(
    vilkår: List<VilkårDagligReise>,
    reiseId: ReiseId,
): String? =
    vilkår
        .mapNotNull { it.fakta }
        .find { fakta ->
            when (fakta) {
                is FaktaOffentligTransport -> fakta.reiseId == reiseId
                is FaktaPrivatBil -> fakta.reiseId == reiseId
            }
        }?.adresse
