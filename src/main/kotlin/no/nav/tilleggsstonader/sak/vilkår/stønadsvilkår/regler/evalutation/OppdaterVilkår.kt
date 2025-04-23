package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.erFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.erSisteDagIMåneden
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.svarTilDomene
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering.utledResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelValidering.validerVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.hentVilkårsregel
import java.time.LocalDate

object OppdaterVilkår {
    /**
     * Oppdaterer [Vilkår] med nye svar og resultat
     * Validerer att svaren er gyldige
     */
    fun validerVilkårOgBeregnResultat(
        vilkår: Vilkår,
        oppdatering: LagreVilkårDto,
    ): RegelResultat {
        val vilkårsregel = hentVilkårsregel(vilkår.type)

        validerVilkår(vilkårsregel, oppdatering.delvilkårsett, vilkår.delvilkårsett)

        val vilkårsresultat = utledResultat(vilkårsregel, oppdatering.delvilkårsett)

        if (oppdatering.erFremtidigUtgift == true) {
            valilderIngenSvarPåFremtidigUtgift(oppdatering)
            return vilkårsresultat
        }

        validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat)
        validerPeriodeOgBeløp(oppdatering, vilkårsresultat)
        validerIngenHøyereUtgifterGrunnetHelsemessigeÅrsaker(oppdatering)
        // Venter på avklaring på hvordan og om vi skal støtte fremtidig utgift
        validerIngenFremtidigUtgift(oppdatering)

        return vilkårsresultat
    }

    private fun validerPeriodeOgBeløp(
        oppdatering: LagreVilkårDto,
        vilkårsresultat: RegelResultat,
    ) {
        val vilkårType = vilkårsresultat.vilkårType
        val resultat = vilkårsresultat.vilkår
        val fom = oppdatering.fom
        val tom = oppdatering.tom
        val vilkårMedUtgift =
            listOf(
                VilkårType.PASS_BARN,
                VilkårType.UTGIFTER_OVERNATTING,
                VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER,
            )
        brukerfeilHvis(fom == null || tom == null) {
            "Mangler fra og med/til og med på vilkår"
        }
        brukerfeilHvisIkke(fom <= tom) {
            "Til og med må være lik eller etter fra og med"
        }
        brukerfeilHvis(
            vilkårType in vilkårMedUtgift &&
                oppdatering.erFremtidigUtgift != true &&
                resultat == Vilkårsresultat.OPPFYLT &&
                oppdatering.utgift == null,
        ) {
            "Mangler utgift på vilkår"
        }
        brukerfeilHvis(oppdatering.erFremtidigUtgift == true && oppdatering.utgift != null) {
            "Kan ikke ha utgift på fremtidig utgift"
        }
        feilHvis(vilkårType !in vilkårMedUtgift && oppdatering.utgift != null) {
            "Kan ikke ha utgift på vilkårType=$vilkårType"
        }
    }

    private fun validerIngenHøyereUtgifterGrunnetHelsemessigeÅrsaker(lagreVilkårDto: LagreVilkårDto) {
        feilHvis(
            lagreVilkårDto.delvilkårsett.any { delvilkår ->
                delvilkår.vurderinger.any {
                    it.regelId ==
                        RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER &&
                        it.svar == SvarId.JA
                }
            },
        ) {
            "Vi støtter ikke beregning med \"Høyere utgifter grunnet helsemessig årsaker\". Ta kontakt med Tilleggsstønader teamet."
        }
    }

    private fun valilderIngenSvarPåFremtidigUtgift(lagreVilkårDto: LagreVilkårDto) {
        feilHvis(
            lagreVilkårDto.delvilkårsett.any { delvilkår ->
                delvilkår.vurderinger.any { vurdering ->
                    vurdering.svar !== null ||
                        vurdering.begrunnelse !== null
                }
            },
        ) {
            "Kan ikke ha svar på vilkår når fremtidig utgift er valgt"
        }
    }

    private fun validerIngenFremtidigUtgift(lagreVilkårDto: LagreVilkårDto) {
        feilHvis(lagreVilkårDto.erFremtidigUtgift == true) {
            "Vi støtter foreløpig ikke fremtidige utgifter"
        }
    }

    fun oppdaterVilkår(
        vilkår: Vilkår,
        oppdatering: LagreVilkårDto,
        vilkårsresultat: RegelResultat,
    ): Vilkår {
        val oppdaterteDelvilkår =
            oppdaterDelvilkår(
                vilkår = vilkår,
                vilkårsresultat = vilkårsresultat,
                oppdatering = oppdatering,
            )
        return vilkår.copy(
            resultat = vilkårsresultat.vilkår,
            status = utledStatus(vilkår),
            delvilkårwrapper = oppdaterteDelvilkår,
            fom = utledFom(vilkår, oppdatering),
            tom = utledTom(vilkår, oppdatering),
            utgift = oppdatering.utgift,
            erFremtidigUtgift = oppdatering.erFremtidigUtgift == true,
            gitVersjon = Applikasjonsversjon.versjon,
        )
    }

    private fun utledFom(
        vilkår: Vilkår,
        oppdatering: LagreVilkårDto,
    ): LocalDate? =
        oppdatering.fom?.let {
            when (vilkår.type) {
                VilkårType.PASS_BARN,
                VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER,
                -> {
                    validerErFørsteDagIMåned(it)
                    it
                }

                VilkårType.UTGIFTER_OVERNATTING -> {
                    it
                }

                else -> error("Har ikke tatt stilling til type dato for ${vilkår.type}")
            }
        }

    private fun utledTom(
        vilkår: Vilkår,
        oppdatering: LagreVilkårDto,
    ): LocalDate? =
        oppdatering.tom?.let {
            when (vilkår.type) {
                VilkårType.PASS_BARN,
                VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER,
                -> {
                    validerErSisteDagIMåned(it)
                    it
                }

                VilkårType.UTGIFTER_OVERNATTING -> {
                    it
                }

                else -> error("Har ikke tatt stilling til type dato for ${vilkår.type}")
            }
        }

    private fun utledStatus(eksisterendeVilkår: Vilkår): VilkårStatus? =
        when (eksisterendeVilkår.status) {
            VilkårStatus.UENDRET -> VilkårStatus.ENDRET
            else -> eksisterendeVilkår.status
        }

    private fun validerErFørsteDagIMåned(dato: LocalDate) {
        require(dato.erFørsteDagIMåneden()) { "Dato=$dato er ikke første dag i måneden" }
    }

    private fun validerErSisteDagIMåned(dato: LocalDate) {
        require(dato.erSisteDagIMåneden()) { "Dato=$dato er ikke siste dag i måneden" }
    }

    private fun validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat: RegelResultat) {
        if (!vilkårsresultat.vilkår.oppfyltEllerIkkeOppfylt()) {
            val message =
                "Mangler fullstendig vilkår for ${vilkårsresultat.vilkårType}. " +
                    "Svar på alle spørsmål samt fyll inn evt. påkrevd begrunnelsesfelt"
            throw Feil(message = message, frontendFeilmelding = message)
        }
    }

    /**
     * Oppdaterer delvilkår
     * Den beholder den opprinnelige rekkefølgen som finnes på delvilkåren i databasen,
     * slik att frontend kan sende inn de i en annen rekkefølge
     *
     * @param vilkår Vilkårsoppdatering fra databasen som skal oppdateres
     */
    private fun oppdaterDelvilkår(
        vilkår: Vilkår,
        vilkårsresultat: RegelResultat,
        oppdatering: LagreVilkårDto,
    ): DelvilkårWrapper {
        val vurderingerPåType = oppdatering.delvilkårsett.associateBy { it.vurderinger.first().regelId }
        val delvilkårsett =
            vilkår.delvilkårsett
                .map {
                    if (it.resultat == Vilkårsresultat.IKKE_AKTUELL) {
                        it
                    } else {
                        val hovedregel = it.hovedregel
                        val resultat = vilkårsresultat.resultatHovedregel(hovedregel)
                        val svar = vurderingerPåType[hovedregel] ?: throw Feil("Savner svar for hovedregel=$hovedregel")

                        if (oppdatering.erFremtidigUtgift == true) {
                            it.copy(
                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                vurderinger = svar.tilTomVurdering(),
                            )
                        } else if (resultat.oppfyltEllerIkkeOppfylt()) {
                            it.copy(
                                resultat = resultat,
                                vurderinger = svar.svarTilDomene(),
                            )
                        } else {
                            throw Feil("Håndterer ikke oppdatering av resultat=$resultat ennå")
                        }
                    }
                }.toList()
        return vilkår.delvilkårwrapper.copy(delvilkårsett = delvilkårsett)
    }

    fun lagNyttVilkår(
        vilkårsregel: Vilkårsregel,
        metadata: HovedregelMetadata,
        behandlingId: BehandlingId,
        barnId: BarnId? = null,
    ): Vilkår {
        val delvilkårsett = vilkårsregel.initiereDelvilkår(metadata, barnId = barnId)
        return Vilkår(
            behandlingId = behandlingId,
            type = vilkårsregel.vilkårType,
            barnId = barnId,
            erFremtidigUtgift = false,
            delvilkårwrapper = DelvilkårWrapper(delvilkårsett),
            resultat = utledResultat(vilkårsregel, delvilkårsett.map { it.tilDto() }).vilkår,
            status = VilkårStatus.NY,
            opphavsvilkår = null,
            gitVersjon = Applikasjonsversjon.versjon,
        )
    }

    private fun DelvilkårDto.tilTomVurdering(): List<Vurdering> =
        this.vurderinger.map {
            Vurdering(
                regelId = it.regelId,
                svar = null,
                begrunnelse = null,
            )
        }
}
