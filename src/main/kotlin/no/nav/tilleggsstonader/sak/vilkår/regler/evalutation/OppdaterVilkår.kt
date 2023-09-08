package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårsvurderingWrapper
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsvurdering
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.dto.svarTilDomene
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregler.Companion.ALLE_VILKÅRSREGLER
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.RegelEvaluering.utledResultat
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.RegelValidering.validerVurdering
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkårsreglerForStønad
import java.util.UUID

object OppdaterVilkår {

    /**
     * Oppdaterer [Vilkårsvurdering] med nye svar og resultat
     * Validerer att svaren er gyldige
     */
    fun lagNyOppdatertVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        oppdatering: List<DelvilkårsvurderingDto>,
        vilkårsregler: Map<VilkårType, Vilkårsregel> = ALLE_VILKÅRSREGLER.vilkårsregler,
    ): Vilkårsvurdering { // TODO: Ikke default input her, kanskje?
        val vilkårsregel =
            vilkårsregler[vilkårsvurdering.type] ?: error("Finner ikke vilkårsregler for ${vilkårsvurdering.type}")

        validerVurdering(vilkårsregel, oppdatering, vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger)

        val vilkårsresultat = utledResultat(vilkårsregel, oppdatering)
        validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat)
        val oppdaterteDelvilkår = oppdaterDelvilkår(vilkårsvurdering, vilkårsresultat, oppdatering)
        return vilkårsvurdering.copy(
            resultat = vilkårsresultat.vilkår,
            delvilkårsvurdering = oppdaterteDelvilkår,
            opphavsvilkår = null,
        )
    }

    private fun validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat: RegelResultat) {
        if (!vilkårsresultat.vilkår.oppfyltEllerIkkeOppfylt()) {
            val message = "Mangler fullstendig vilkårsvurdering for ${vilkårsresultat.vilkårType}. " +
                "Svar på alle spørsmål samt fyll inn evt. påkrevd begrunnelsesfelt"
            throw Feil(message = message, frontendFeilmelding = message)
        }
    }

    /**
     * Oppdaterer delvilkår
     * Den beholder den opprinnelige rekkefølgen som finnes på delvilkåren i databasen,
     * slik att frontend kan sende inn de i en annen rekkefølge
     *
     * @param vilkårsvurdering Vilkårsoppdatering fra databasen som skal oppdateres
     */
    private fun oppdaterDelvilkår(
        vilkårsvurdering: Vilkårsvurdering,
        vilkårsresultat: RegelResultat,
        oppdatering: List<DelvilkårsvurderingDto>,
    ): DelvilkårsvurderingWrapper {
        val vurderingerPåType = oppdatering.associateBy { it.vurderinger.first().regelId }
        val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.map {
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
        return vilkårsvurdering.delvilkårsvurdering.copy(delvilkårsvurderinger = delvilkårsvurderinger)
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

    fun utledResultatForVilkårSomGjelderFlereBarn(value: List<Vilkårsvurdering>): Vilkårsresultat {
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

    fun utledBehandlingKategori(vilkårsvurderinger: List<Vilkårsvurdering>): BehandlingKategori {
        return BehandlingKategori.NASJONAL
    }
    /*
    val medlemFolketrygd =
        vilkårsvurderinger.utledVurderinger(VilkårType.FORUTGÅENDE_MEDLEMSKAP, RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN)
            .harSvar(SvarId.JA)

    val unntakEøsAnnenForelder =
        vilkårsvurderinger.utledVurderinger(VilkårType.FORUTGÅENDE_MEDLEMSKAP, RegelId.MEDLEMSKAP_UNNTAK)
            .harSvar(SvarId.MEDLEM_MER_ENN_5_ÅR_EØS_ANNEN_FORELDER_TRYGDEDEKKET_I_NORGE)

    val unntakEøsMedlemskap =
        vilkårsvurderinger.utledVurderinger(VilkårType.FORUTGÅENDE_MEDLEMSKAP, RegelId.MEDLEMSKAP_UNNTAK)
            .harSvar(SvarId.MEDLEM_MER_ENN_5_ÅR_EØS)

    val borOgOppholderSegINorge =
        vilkårsvurderinger.utledVurderinger(VilkårType.LOVLIG_OPPHOLD, RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE)
            .harSvar(SvarId.JA)

    val unntakEøsOpphold =
        vilkårsvurderinger.utledVurderinger(VilkårType.LOVLIG_OPPHOLD, RegelId.OPPHOLD_UNNTAK)
            .harSvar(SvarId.OPPHOLDER_SEG_I_ANNET_EØS_LAND)

    val forutgåendeMedelmskapUtløserEøs =
        !medlemFolketrygd && (unntakEøsAnnenForelder || unntakEøsMedlemskap)
    val lovligOppholdUtløserEøs = !borOgOppholderSegINorge && unntakEøsOpphold

    return if (forutgåendeMedelmskapUtløserEøs || lovligOppholdUtløserEøs) BehandlingKategori.EØS else BehandlingKategori.NASJONAL
}
*/

    fun erAlleVilkårsvurderingerOppfylt(
        vilkårsvurderinger: List<Vilkårsvurdering>,
        stønadstype: Stønadstype,
    ): Boolean {
        val inneholderAlleTyperVilkår =
            vilkårsvurderinger.map { it.type }.containsAll(VilkårType.hentVilkårForStønad(stønadstype))
        val vilkårsresultat = utledVilkårsresultat(vilkårsvurderinger)
        return inneholderAlleTyperVilkår && vilkårsresultat.all { it == Vilkårsresultat.OPPFYLT }
    }

    private fun utledVilkårsresultat(lagredeVilkårsvurderinger: List<Vilkårsvurdering>): List<Vilkårsresultat> {
        val vilkårsresultat = lagredeVilkårsvurderinger.groupBy { it.type }.map {
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

    fun opprettNyeVilkårsvurderinger(
        behandlingId: UUID,
        metadata: HovedregelMetadata,
        stønadstype: Stønadstype,
    ): List<Vilkårsvurdering> {
        return vilkårsreglerForStønad(stønadstype)
            .flatMap { vilkårsregel ->
                if (vilkårsregel.vilkårType.gjelderFlereBarn() && metadata.barn.isNotEmpty()) {
                    metadata.barn.map { lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId, it.id) }
                } else {
                    listOf(lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId))
                }
            }
    }

    fun lagVilkårsvurderingForNyttBarn(
        metadata: HovedregelMetadata,
        behandlingId: UUID,
        barnId: UUID,
        stønadstype: Stønadstype,
    ): List<Vilkårsvurdering> {
        return emptyList()
    }
    /*
    return when (stønadstype) {
        OVERGANGSSTØNAD, SKOLEPENGER -> listOf(
            lagNyVilkårsvurdering(
                AleneomsorgRegel(),
                metadata,
                behandlingId,
                barnId,
            ),
        )

        BARNETILSYN -> listOf(
            lagNyVilkårsvurdering(AleneomsorgRegel(), metadata, behandlingId, barnId),
            lagNyVilkårsvurdering(AlderPåBarnRegel(), metadata, behandlingId, barnId),
        )
    }
    }
     */

    private fun lagNyVilkårsvurdering(
        vilkårsregel: Vilkårsregel,
        metadata: HovedregelMetadata,
        behandlingId: UUID,
        barnId: UUID? = null,
    ): Vilkårsvurdering {
        val delvilkårsvurdering = vilkårsregel.initiereDelvilkårsvurdering(metadata, barnId = barnId)
        return Vilkårsvurdering(
            behandlingId = behandlingId,
            type = vilkårsregel.vilkårType,
            barnId = barnId,
            delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering),
            resultat = utledResultat(vilkårsregel, delvilkårsvurdering.map { it.tilDto() }).vilkår,
            opphavsvilkår = null,
        )
    }
}
