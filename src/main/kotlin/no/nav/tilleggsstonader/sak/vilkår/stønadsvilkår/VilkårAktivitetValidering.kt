package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import java.time.LocalDate

/** Felles validering for stønadsvilkår som knyttes til en bestemt aktivitet. */
object VilkårAktivitetValidering {
    fun validerAktivitetOppfyltHeleVilkåret(
        aktivitet: VilkårperiodeAktivitet?,
        vilkårsperiode: Periode<LocalDate>,
    ) {
        brukerfeilHvis(aktivitet == null) {
            "Aktiviteten finnes ikke"
        }
        brukerfeilHvis(aktivitet.resultat != ResultatVilkårperiode.OPPFYLT) {
            "Aktiviteten er ikke oppfylt"
        }
        brukerfeilHvisIkke(aktivitet.inneholder(vilkårsperiode)) {
            "Aktiviteten er ikke oppfylt hele vilkårperioden"
        }
    }
}
