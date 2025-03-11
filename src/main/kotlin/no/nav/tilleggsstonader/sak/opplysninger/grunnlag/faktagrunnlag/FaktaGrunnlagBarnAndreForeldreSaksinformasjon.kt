package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn

/**
 * @param andreForeldre gjelder andre foreldre som denne saken ikke gjelder.
 * Dvs hvis denne saken gjelder mor vil annenForelder inneholde FAR
 */
data class FaktaGrunnlagBarnAndreForeldreSaksinformasjon(
    val identBarn: String,
    val andreForeldre: List<FaktaGrunnlagAnnenForelderSaksinformasjon>,
) : FaktaGrunnlagData {
    override val type: TypeFaktaGrunnlag = TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON
}

/**
 * @param ident ident på annen forelder
 * @param harBehandlingUnderArbeid har behandling under arbeid når grunnlag blir opprettet
 * @param vedtaksperioderBarn barn som finnes på denne behandlingen som også finnes på en annan sak
 */
data class FaktaGrunnlagAnnenForelderSaksinformasjon(
    val ident: String,
    val harBehandlingUnderArbeid: Boolean,
    val vedtaksperioderBarn: List<Datoperiode>,
)

private typealias IdentBarn = String
private typealias IdentAnnenForelder = String

/**
 * Hjelpeklasse for å mappe informasjon om forelder til
 * [FaktaGrunnlagBarnAndreForeldreSaksinformasjon] og [FaktaGrunnlagAnnenForelderSaksinformasjon]
 */
data class BehandlingsinformasjonAnnenForelder(
    val identForelder: IdentAnnenForelder,
    val finnesIkkeFerdigstiltBehandling: Boolean,
    val iverksattBehandling: IverksattBehandlingForelder?,
) {
    class IverksattBehandlingForelder(
        private val barn: Map<BarnId, IdentBarn>,
        private val vedtak: InnvilgelseEllerOpphørTilsynBarn,
    ) {
        val perioderPerBarnIdent: Map<IdentBarn, List<Datoperiode>> by lazy {
            val perioderForBarn: MutableMap<BarnId, MutableList<Datoperiode>> = mutableMapOf()
            vedtak.beregningsresultat.perioder
                .forEach { perioder ->

                    perioder.grunnlag.vedtaksperiodeGrunnlag.forEach { vedtaksperiode ->
                        val fom = vedtaksperiode.vedtaksperiode.fom
                        val tom = vedtaksperiode.vedtaksperiode.tom
                        perioder.grunnlag.utgifter.forEach { utgift ->
                            perioderForBarn
                                .getOrPut(utgift.barnId) { mutableListOf() }
                                .add(Datoperiode(fom = fom, tom = tom))
                        }
                    }
                }
            perioderForBarn
                .mapKeys { barn[it.key]!! }
                .mapValues { it.value.sorted().mergeSammenhengende { d1, d2 -> d1.overlapperEllerPåfølgesAv(d2) } }
        }
    }
}

object FaktaGrunnlagBarnAndreForeldreSaksinformasjonMapper {
    fun mapBarnAndreForeldreSaksinformasjon(
        behandlingId: BehandlingId,
        barnAnnenForelder: Map<IdentBarn, List<IdentAnnenForelder>>,
        behandlingsinformasjonAnnenForelder: List<BehandlingsinformasjonAnnenForelder>,
    ): List<GeneriskFaktaGrunnlag<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>> {
        val behandlingsinformasjon = behandlingsinformasjonAnnenForelder.associateBy { it.identForelder }
        return barnAnnenForelder
            .mapBarnForelderInformasjon(behandlingsinformasjon)
            .map { (identBarn, barnForelderInformasjon) ->
                mapFaktaGrunnlag(behandlingId, identBarn, barnForelderInformasjon)
            }
    }

    private fun Map<IdentBarn, List<IdentAnnenForelder>>.mapBarnForelderInformasjon(
        behandlingsinformasjonAnnenForelder: Map<String, BehandlingsinformasjonAnnenForelder>,
    ) = this.mapValues { (identBarn, identerAnnenForelder) ->
        identerAnnenForelder.map { identAnnenForelder ->
            val forelderInformasjon = behandlingsinformasjonAnnenForelder[identAnnenForelder]
            BarnForelderInformasjon(
                identForelder = identAnnenForelder,
                finnesIkkeFerdigstiltBehandling = forelderInformasjon?.finnesIkkeFerdigstiltBehandling ?: false,
                vedtaksperioderBarn =
                    forelderInformasjon?.iverksattBehandling?.perioderPerBarnIdent?.get(identBarn)
                        ?: emptyList(),
            )
        }
    }

    private fun mapFaktaGrunnlag(
        behandlingId: BehandlingId,
        identBarn: IdentBarn,
        barnForelderInformasjon: List<BarnForelderInformasjon>,
    ) = GeneriskFaktaGrunnlag(
        behandlingId = behandlingId,
        type = TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON,
        typeId = identBarn,
        data = faktaGrunnlagBarnAnnenForelder(identBarn, barnForelderInformasjon),
    )

    private fun faktaGrunnlagBarnAnnenForelder(
        identBarn: IdentBarn,
        barnForelderInformasjon: List<BarnForelderInformasjon>,
    ) = FaktaGrunnlagBarnAndreForeldreSaksinformasjon(
        identBarn = identBarn,
        andreForeldre =
            barnForelderInformasjon.map {
                FaktaGrunnlagAnnenForelderSaksinformasjon(
                    ident = it.identForelder,
                    harBehandlingUnderArbeid = it.finnesIkkeFerdigstiltBehandling,
                    vedtaksperioderBarn = it.vedtaksperioderBarn,
                )
            },
    )

    private data class BarnForelderInformasjon(
        val identForelder: String,
        val finnesIkkeFerdigstiltBehandling: Boolean,
        val vedtaksperioderBarn: List<Datoperiode>,
    )
}
