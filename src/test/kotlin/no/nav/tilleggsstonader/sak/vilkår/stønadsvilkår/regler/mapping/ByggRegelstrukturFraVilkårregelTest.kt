package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping

import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregler
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import org.junit.jupiter.api.Test

class ByggRegelstrukturFraVilkårregelTest {
    /**
     * Feiler hvis regeltreeet har endret seg. Var det meningen, kjør
     *      SKRIV_TIL_FIL=true ./gradlew :test --tests "no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregelTest.regelstrukturSnapshotTest"
     * OBS: Ved oppdatering av filene må testen kjøres flere ganger fordi den feiler per fil som endres.
     */
    @Test
    fun regelstrukturSnapshotTest() {
        val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.map { it.value }

        vilkårsregler.forEach {
            assertFileJsonIsEqual("vilkår/regelstruktur/${it.vilkårType}.json", it.tilRegelstruktur())
        }
    }
}
