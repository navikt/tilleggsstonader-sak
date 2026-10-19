package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseOppstartAvslutningHjemreiseRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.RegelstrukturReiseOppstartAvslutningHjemreiseDto
import org.junit.jupiter.api.Test

class ReiseOppstartAvslutningHjemreiseRegelstrukturSnapshotTest {
    @Test
    fun `regelstruktur api snapshot test`() {
        val regelstruktur =
            RegelstrukturReiseOppstartAvslutningHjemreiseDto(
                oppstart = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.OPPSTART).tilRegelstruktur(),
                avslutning = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.AVSLUTNING).tilRegelstruktur(),
                hjemreise = ReiseOppstartAvslutningHjemreiseRegel.forType(TypeReiseformål.HJEMREISE).tilRegelstruktur(),
            )

        assertFileJsonIsEqual("vilkår/regelstruktur/REISE_OPPSTART_AVSLUTNING_HJEMREISE_API.json", regelstruktur)
    }
}
