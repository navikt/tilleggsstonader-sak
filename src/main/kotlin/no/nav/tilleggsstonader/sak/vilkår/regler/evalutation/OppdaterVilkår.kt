package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.svarTilDomene
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregler.Companion.ALLE_VILKÅRSREGLER
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.RegelEvaluering.utledResultat
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.RegelValidering.validerVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkårsreglerForStønad
import java.util.UUID

object OppdaterVilkår {

    /**
     * Oppdaterer [Vilkår] med nye svar og resultat
     * Validerer att svaren er gyldige
     */
    fun validerOgOppdatertVilkår(
        vilkår: Vilkår,
        oppdatering: List<DelvilkårDto>,
        vilkårsregler: Map<VilkårType, Vilkårsregel> = ALLE_VILKÅRSREGLER.vilkårsregler,
    ): Vilkår { // TODO: Ikke default input her, kanskje?
        val vilkårsregel =
            vilkårsregler[vilkår.type] ?: error("Finner ikke vilkårsregler for ${vilkår.type}")

        validerVilkår(vilkårsregel, oppdatering, vilkår.delvilkårsett)

        val vilkårsresultat = utledResultat(vilkårsregel, oppdatering)
        validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat)
        val oppdaterteDelvilkår = oppdaterDelvilkår(vilkår, vilkårsresultat, oppdatering)
        return vilkår.copy(
            resultat = vilkårsresultat.vilkår,
            delvilkårwrapper = oppdaterteDelvilkår,
            opphavsvilkår = null,
        )
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
        oppdatering: List<DelvilkårDto>,
    ): DelvilkårWrapper {
        val vurderingerPåType = oppdatering.associateBy { it.vurderinger.first().regelId }
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
                    // TODO håndtering for [Vilkårsresultat.SKAL_IKKE_VURDERES] som burde beholde første svaret i det delvilkåret
                    throw Feil("Håndterer ikke oppdatering av resultat=$resultat ennå")
                }
            }
        }.toList()
        return vilkår.delvilkårwrapper.copy(delvilkårsett = delvilkårsett)
    }

    /**
     * Et vilkår skal anses som vurdert dersom det er oppfylt eller saksbehandler har valgt å ikke vurdere det
     */
    fun erAlleVilkårTattStillingTil(vilkårsresultat: List<Vilkårsresultat>): Boolean {
        return if (vilkårsresultat.all { it == Vilkårsresultat.OPPFYLT || it == Vilkårsresultat.SKAL_IKKE_VURDERES }) {
            true
        } else {
            harNoenIkkeOppfyltOgRestenIkkeOppfyltEllerOppfyltEllerSkalIkkevurderes(vilkårsresultat)
        }
    }

    fun utledResultatForVilkårSomGjelderFlereBarn(value: List<Vilkår>): Vilkårsresultat {
        feilHvis(value.any { !it.type.gjelderFlereBarn() }) {
            "Denne metoden kan kun kalles med vilkår som kan ha flere barn"
        }
        return when {
            value.any { it.resultat == Vilkårsresultat.OPPFYLT } -> Vilkårsresultat.OPPFYLT
            value.all { it.barnId == null && it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL } -> Vilkårsresultat.SKAL_IKKE_VURDERES // Dersom man ikke har barn på behandlingen så er ikke disse vilkårene aktuelle å vurdere
            value.any { it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL } -> Vilkårsresultat.IKKE_TATT_STILLING_TIL
            value.all { it.resultat == Vilkårsresultat.SKAL_IKKE_VURDERES } -> Vilkårsresultat.SKAL_IKKE_VURDERES
            value.any { it.resultat == Vilkårsresultat.IKKE_OPPFYLT } &&
                value.all { it.resultat == Vilkårsresultat.IKKE_OPPFYLT || it.resultat == Vilkårsresultat.SKAL_IKKE_VURDERES } ->
                Vilkårsresultat.IKKE_OPPFYLT

            else -> throw Feil(
                "Utled resultat for aleneomsorg - kombinasjon av resultat er ikke behandlet: " +
                    "${value.map { it.resultat }}",
            )
        }
    }

    fun utledBehandlingKategori(vilkårsett: List<Vilkår>): BehandlingKategori {
        return BehandlingKategori.NASJONAL
    }
    /*
    val medlemFolketrygd =
        vilkårsett.utledVurderinger(VilkårType.FORUTGÅENDE_MEDLEMSKAP, RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN)
            .harSvar(SvarId.JA)

    val unntakEøsAnnenForelder =
        vilkårsett.utledVurderinger(VilkårType.FORUTGÅENDE_MEDLEMSKAP, RegelId.MEDLEMSKAP_UNNTAK)
            .harSvar(SvarId.MEDLEM_MER_ENN_5_ÅR_EØS_ANNEN_FORELDER_TRYGDEDEKKET_I_NORGE)

    val unntakEøsMedlemskap =
        vilkårsett.utledVurderinger(VilkårType.FORUTGÅENDE_MEDLEMSKAP, RegelId.MEDLEMSKAP_UNNTAK)
            .harSvar(SvarId.MEDLEM_MER_ENN_5_ÅR_EØS)

    val borOgOppholderSegINorge =
        vilkårsett.utledVurderinger(VilkårType.LOVLIG_OPPHOLD, RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE)
            .harSvar(SvarId.JA)

    val unntakEøsOpphold =
        vilkårsett.utledVurderinger(VilkårType.LOVLIG_OPPHOLD, RegelId.OPPHOLD_UNNTAK)
            .harSvar(SvarId.OPPHOLDER_SEG_I_ANNET_EØS_LAND)

    val forutgåendeMedelmskapUtløserEøs =
        !medlemFolketrygd && (unntakEøsAnnenForelder || unntakEøsMedlemskap)
    val lovligOppholdUtløserEøs = !borOgOppholderSegINorge && unntakEøsOpphold

    return if (forutgåendeMedelmskapUtløserEøs || lovligOppholdUtløserEøs) BehandlingKategori.EØS else BehandlingKategori.NASJONAL
}
*/

    fun erAlleVilkårOppfylt(
        vilkårsett: List<Vilkår>,
        stønadstype: Stønadstype,
    ): Boolean {
        val inneholderAlleTyperVilkår =
            vilkårsett.map { it.type }.containsAll(VilkårType.hentVilkårForStønad(stønadstype))
        val vilkårsresultat = utledVilkårsresultat(vilkårsett)
        return inneholderAlleTyperVilkår && vilkårsresultat.all { it == Vilkårsresultat.OPPFYLT }
    }

    private fun utledVilkårsresultat(lagretVilkårsett: List<Vilkår>): List<Vilkårsresultat> {
        val vilkårsresultat = lagretVilkårsett.groupBy { it.type }.map {
            if (it.key.gjelderFlereBarn()) {
                utledResultatForVilkårSomGjelderFlereBarn(it.value)
            } else {
                it.value.single().resultat
            }
        }
        return vilkårsresultat
    }

    /**
     * [Vilkårsresultat.IKKE_OPPFYLT] er gyldig i kombinasjon med andre som er
     * [Vilkårsresultat.IKKE_OPPFYLT], [Vilkårsresultat.OPPFYLT] og [Vilkårsresultat.SKAL_IKKE_VURDERES]
     */
    private fun harNoenIkkeOppfyltOgRestenIkkeOppfyltEllerOppfyltEllerSkalIkkevurderes(vilkårsresultat: List<Vilkårsresultat>) =
        vilkårsresultat.any { it == Vilkårsresultat.IKKE_OPPFYLT } &&
            vilkårsresultat.all {
                it == Vilkårsresultat.OPPFYLT ||
                    it == Vilkårsresultat.IKKE_OPPFYLT ||
                    it == Vilkårsresultat.SKAL_IKKE_VURDERES
            }

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
                    metadata.barn.map { lagNyVilkår(vilkårsregel, metadata, behandlingId, it.id) }
                } else {
                    listOf(lagNyVilkår(vilkårsregel, metadata, behandlingId))
                }
            }
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

    private fun lagNyVilkår(
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
