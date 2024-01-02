package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class MålgruppeOmstillingsstønadRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE_OMSTILLINGSSTØNAD,
    regler = setOf(OMSTILLINGSSTØNAD_LEDD),
    hovedregler = regelIder(OMSTILLINGSSTØNAD_LEDD),
)

private val OMSTILLINGSSTØNAD_LEDD =
    RegelSteg(
        regelId = RegelId.OMSTILLINGSSTØNAD_LEDD,
        svarMapping = mapOf(
            SvarId.FØRSTE_LEDD to SluttSvarRegel.OPPFYLT,
            SvarId.ANDRE_LEDD to SluttSvarRegel.IKKE_OPPFYLT,
        ),
    )
