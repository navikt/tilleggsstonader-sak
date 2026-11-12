package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet

/** Validering av vilkår for reise oppstart, avslutning og hjemreise (ROAH). Uten oppslag, slik at den kan enhetstestes. */
object ROAHVilkårValideringUtil {
    private val ROAH_VILKÅRSTYPER =
        setOf(
            VilkårType.ROAH_OPPSTART_AVSLUTNING,
            VilkårType.ROAH_HJEMREISE,
            VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
        )

    /**
     * Endepunktet kan bare oppdatere vilkår for reise oppstart, avslutning og hjemreise, ikke andre vilkårstyper.
     * En reise kan heller ikke bli et paraplyvilkår, eller omvendt, ved oppdatering.
     */
    fun validerKanOppdatereVilkår(
        eksisterendeVilkår: Vilkår,
        nyttVilkår: LagreVilkårROAH,
    ) {
        feilHvis(eksisterendeVilkår.type !in ROAH_VILKÅRSTYPER) {
            "Vilkåret er ikke et vilkår for reise oppstart, avslutning og hjemreise"
        }
        val erEksisterendeVilkårParaplyvilkår = eksisterendeVilkår.type == VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE
        val erNyttVilkårParaplyvilkår = nyttVilkår is LagreVilkårMidlertidigBosted
        feilHvis(erEksisterendeVilkårParaplyvilkår != erNyttVilkårParaplyvilkår) {
            "Kan ikke endre mellom reise og paraplyvilkår"
        }
    }

    /**
     * Aktivitet valideres likt for TSO og TSR (i motsetning til reise til samling, der dette kun valideres for TSR).
     */
    fun validerAktivitet(
        nyttVilkår: LagreVilkårROAH,
        aktivitet: VilkårperiodeAktivitet?,
    ) {
        brukerfeilHvis(aktivitet == null) {
            "Aktiviteten finnes ikke"
        }
        brukerfeilHvis(aktivitet.resultat != ResultatVilkårperiode.OPPFYLT) {
            "Aktiviteten er ikke oppfylt"
        }
        brukerfeilHvisIkke(aktivitet.inneholder(nyttVilkår)) {
            "Aktiviteten er ikke oppfylt hele vilkårperioden"
        }
    }

    /**
     * En reise kan høre til et paraplyvilkår (midlertidig bosted), referert med paraplyvilkårets globalId.
     * [parentVilkår] er paraplyvilkåret slått opp i samme behandling, eller null hvis det ikke finnes.
     * [vilkårId] er null når vilkåret opprettes. Feilmeldingene inneholder ingen verdier fra vilkåret.
     */
    fun validerTilhørendeParaplyvilkår(
        nyttVilkår: LagreVilkårReiseOppstartAvslutningHjemreise,
        parentVilkår: Vilkår?,
        vilkårId: VilkårId?,
    ) {
        feilHvis(parentVilkår == null) { "Paraplyvilkåret finnes ikke i behandlingen" }
        feilHvis(parentVilkår.id == vilkårId) { "Et vilkår kan ikke referere til seg selv" }
        feilHvis(parentVilkår.status == VilkårStatus.SLETTET) { "Paraplyvilkåret er slettet" }
        feilHvis(parentVilkår.type != VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE) {
            "Referansen peker ikke på et paraplyvilkår"
        }

        val paraply = parentVilkår.mapTilVilkårROAH()
        feilHvis(paraply.aktivitetId != nyttVilkår.aktivitetId) {
            "Reisen må høre til samme aktivitet som paraplyvilkåret"
        }
        brukerfeilHvisIkke(paraply.inneholder(nyttVilkår)) {
            "Reisens periode må være innenfor paraplyvilkårets periode"
        }
    }
}
