package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping

import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregler
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import org.junit.jupiter.api.Test

class ByggRegelstrukturFraVilkårregelTest {
    /**
     * Sett FileUtil.SKAL_SKRIVE_TIL_FIL = true for å oppdatere filene ved endring som er ment å påvirke logikken.
     * Kan også gjøres for å lage nye filer dersom det legges til nye vilkårsregler.
     * OBS: Ved oppdatering av filene må testen kjøres flere ganger fordi den feiler per fil som endres.
     */
    @Test
    internal fun `sjekker at output fortsatt er det samme på json`() {
        val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.map { it.value }

        vilkårsregler.forEach {
            assertFileJsonIsEqual("vilkår/regelstruktur/${it.vilkårType}.json", it.tilRegelstruktur())
        }
    }
}
