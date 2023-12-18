package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel

val NEDSATT_ARBEIDSEVNE =
    RegelSteg(
        regelId = RegelId.NEDSATT_ARBEIDSEVNE,
        jaNeiSvarRegel(
            hvisJa = SluttSvarRegel.OPPFYLT,
            hvisNei = SluttSvarRegel.IKKE_OPPFYLT,
        ),
    )
