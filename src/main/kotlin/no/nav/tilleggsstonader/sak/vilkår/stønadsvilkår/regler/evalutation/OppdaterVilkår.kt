package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.erFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.erSisteDagIMåneden
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.svarTilDomene
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelEvaluering.utledResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.RegelValidering.validerVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.hentVilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkårsreglerForStønad
import java.time.LocalDate
import java.util.UUID

object OppdaterVilkår {

    /**
     * Oppdaterer [Vilkår] med nye svar og resultat
     * Validerer att svaren er gyldige
     */
    fun validerVilkårOgBeregnResultat(
        vilkår: Vilkår,
        oppdatering: LagreVilkårDto,
        toggleVilkårPeriodiseringEnabled: Boolean,
    ): RegelResultat {
        val vilkårsregel = hentVilkårsregel(vilkår.type)

        validerVilkår(vilkårsregel, oppdatering.delvilkårsett, vilkår.delvilkårsett)

        val vilkårsresultat = utledResultat(vilkårsregel, oppdatering.delvilkårsett)
        validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat)
        validerPeriodeOgBeløp(oppdatering, vilkårsresultat, toggleVilkårPeriodiseringEnabled)

        return vilkårsresultat
    }

    private fun validerPeriodeOgBeløp(
        oppdatering: LagreVilkårDto,
        vilkårsresultat: RegelResultat,
        toggleVilkårPeriodiseringEnabled: Boolean,
    ) {
        if (toggleVilkårPeriodiseringEnabled) {
            val vilkårType = vilkårsresultat.vilkårType
            val resultat = vilkårsresultat.vilkår
            val fom = oppdatering.fom
            val tom = oppdatering.tom
            brukerfeilHvis(fom == null || tom == null) {
                "Mangler fra og med/til og med på vilkår"
            }
            brukerfeilHvisIkke(fom <= tom) {
                "Til og med må være lik eller etter fra og med"
            }
            brukerfeilHvis(
                vilkårType == VilkårType.PASS_BARN &&
                    resultat == Vilkårsresultat.OPPFYLT &&
                    oppdatering.utgift == null,
            ) {
                "Mangler utgift på vilkår"
            }
            feilHvis(vilkårType != VilkårType.PASS_BARN && oppdatering.utgift != null) {
                "Kan ikke ha utgift på vilkårType=$vilkårType"
            }
        }
    }

    fun oppdaterVilkår(
        vilkår: Vilkår,
        oppdatering: LagreVilkårDto,
        vilkårsresultat: RegelResultat,
    ): Vilkår {
        val oppdaterteDelvilkår = oppdaterDelvilkår(
            vilkår = vilkår,
            vilkårsresultat = vilkårsresultat,
            validerteDelvilkårsett = oppdatering.delvilkårsett,
        )
        return vilkår.copy(
            resultat = vilkårsresultat.vilkår,
            delvilkårwrapper = oppdaterteDelvilkår,
            opphavsvilkår = null,
            fom = utledFom(vilkår, oppdatering),
            tom = utledTom(vilkår, oppdatering),
            utgift = oppdatering.utgift,
        )
    }

    private fun utledFom(vilkår: Vilkår, oppdatering: LagreVilkårDto): LocalDate? {
        return oppdatering.fom?.let {
            when (vilkår.type) {
                VilkårType.PASS_BARN -> {
                    validerErFørsteDagIMåned(it)
                    it
                }
                else -> error("Har ikke tatt stilling til type dato for ${vilkår.type}")
            }
        }
    }

    private fun utledTom(vilkår: Vilkår, oppdatering: LagreVilkårDto): LocalDate? {
        return oppdatering.tom?.let {
            when (vilkår.type) {
                VilkårType.PASS_BARN -> {
                    validerErSisteDagIMåned(it)
                    it
                }
                else -> error("Har ikke tatt stilling til type dato for ${vilkår.type}")
            }
        }
    }

    private fun validerErFørsteDagIMåned(dato: LocalDate) {
        require(dato.erFørsteDagIMåneden()) { "Dato=$dato er ikke første dag i måneden" }
    }

    private fun validerErSisteDagIMåned(dato: LocalDate) {
        require(dato.erSisteDagIMåneden()) { "Dato=$dato er ikke siste dag i måneden" }
    }

    private fun validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat: RegelResultat) {
        if (!vilkårsresultat.vilkår.oppfyltEllerIkkeOppfylt()) {
            val message = "Mangler fullstendig vilkår for ${vilkårsresultat.vilkårType}. " +
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
        validerteDelvilkårsett: List<DelvilkårDto>,
    ): DelvilkårWrapper {
        val vurderingerPåType = validerteDelvilkårsett.associateBy { it.vurderinger.first().regelId }
        val delvilkårsett = vilkår.delvilkårsett.map {
            if (it.resultat == Vilkårsresultat.IKKE_AKTUELL) {
                it
            } else {
                val hovedregel = it.hovedregel
                val resultat = vilkårsresultat.resultatHovedregel(hovedregel)
                val svar = vurderingerPåType[hovedregel] ?: throw Feil("Savner svar for hovedregel=$hovedregel")

                if (resultat.oppfyltEllerIkkeOppfylt()) {
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

    // TODO rename noe stønadsspesifikt vilkår
    fun opprettNyeVilkår(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
        stønadstype: Stønadstype,
    ): List<Vilkår> {
        return vilkårsreglerForStønad(stønadstype)
            .flatMap { vilkårsregel ->
                feilHvis(vilkårsregel.vilkårType.gjelderFlereBarn() && metadata.barn.isEmpty()) {
                    "Kan ikke opprette vilkår når ingen barn er knyttet til behandling $behandlingId"
                }

                if (vilkårsregel.vilkårType.gjelderFlereBarn()) {
                    metadata.barn.map { lagNyttVilkår(vilkårsregel, metadata, behandlingId, it.id) }
                } else {
                    listOf(lagNyttVilkår(vilkårsregel, metadata, behandlingId))
                }
            }
    }

    fun opprettVilkårForNyeBarn(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
        stønadstype: Stønadstype,
        eksisterendeVilkår: List<Vilkår>,
    ): List<Vilkår> {
        return vilkårsreglerForStønad(stønadstype)
            .filter { it.vilkårType.gjelderFlereBarn() }
            .mapNotNull { vilkårsregel ->
                metadata.barn.filter { barn ->
                    eksisterendeVilkår.none {
                        val vilkårFinnesForBarn = it.barnId == barn.id && it.type == vilkårsregel.vilkårType

                        vilkårFinnesForBarn
                    }
                }.map { lagNyttVilkår(vilkårsregel, metadata, behandlingId, it.id) }
            }.flatten()
    }

    fun lagVilkårForNyttBarn(
        metadata: HovedregelMetadata,
        behandlingId: UUID,
        barnId: UUID,
        stønadstype: Stønadstype,
    ): List<Vilkår> {
        return emptyList()
    }
    /*
    return when (stønadstype) {
        OVERGANGSSTØNAD, SKOLEPENGER -> listOf(
            lagNyVilkår(
                AleneomsorgRegel(),
                metadata,
                behandlingId,
                barnId,
            ),
        )

        BARNETILSYN -> listOf(
            lagNyVilkår(AleneomsorgRegel(), metadata, behandlingId, barnId),
            lagNyVilkår(AlderPåBarnRegel(), metadata, behandlingId, barnId),
        )
    }
    }
     */

    fun lagNyttVilkår(
        vilkårsregel: Vilkårsregel,
        metadata: HovedregelMetadata,
        behandlingId: UUID,
        barnId: UUID? = null,
    ): Vilkår {
        val delvilkårsett = vilkårsregel.initiereDelvilkår(metadata, barnId = barnId)
        return Vilkår(
            behandlingId = behandlingId,
            type = vilkårsregel.vilkårType,
            barnId = barnId,
            delvilkårwrapper = DelvilkårWrapper(delvilkårsett),
            resultat = utledResultat(vilkårsregel, delvilkårsett.map { it.tilDto() }).vilkår,
            opphavsvilkår = null,
        )
    }
}
