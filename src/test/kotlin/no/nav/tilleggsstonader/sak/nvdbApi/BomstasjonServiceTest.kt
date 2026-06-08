package no.nav.tilleggsstonader.sak.nvdbApi

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.nvdbApi.BomstasjonService
import no.nav.tilleggsstonader.sak.nvdbApi.NvdbBomstasjon
import no.nav.tilleggsstonader.sak.nvdbApi.NvdbBomstasjonClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BomstasjonServiceTest {
    private val nvdbBomstasjonClient = mockk<NvdbBomstasjonClient>()
    private val bomstasjonService = BomstasjonService(nvdbBomstasjonClient)

    // Polyline fra Oslo-ruten: fra (59.9230926, 10.755531) til (59.93544, 10.6964283)
    private val osloPolyline =
        "ivvlJaus`AiAFmEZKFiCpAo@@CsFcDVI?aBLCCy@oCCgA[@aDHa@?BpCBbB?J?LBzC?F@p@BzF@zBBzD@v@@H@b@@f@@hAG~CAFg@" +
            "JYHKDuBh@aEbAkCTG?G@oA?wAKE?{Bg@y@e@QKq@a@a@GSAQ?e@Cy@CU?i@GWCS?Kn@k@bD[bCI|@Gj@ItAG|BGvEC~AChAAl@" +
            "AVAt@Cf@MjCIrAKdB_@~G_AhQS~DQhDC^KtBQ`DGdA?tA?\\@VHpAP`B@JDPBNx@`D~@vDh@jBRt@r@lBLZXv@N^HRr@hBPd@Nf@" +
            "h@fB@BVj@Zf@r@zAfBnEr@hBVr@N^Tj@d@nAHZf@~Ad@|ABLl@nB\\pA\\dAPl@jA`Ex@xCVz@BHHZt@fCTx@^rAPr@V~@J`ADPDN" +
            "b@zALzACn@g@tAg@dAa@z@INKHaBxCGTOVs@|Aq@lBMf@Md@APYzAm@pEm@lEAb@iA|I[rBGLMPELCR?N@LGd@[dAy@dCKNSd@" +
            "ADo@jAGFCAKDCFEP?J@LYhAA@SdAi@bDU`BOjASxAiBxNCJK`BKrAUtCc@CSCmAI"

    @Test
    fun `harBomvei returnerer true når bomstasjon er nær starten av ruten`() {
        // Plassert ~4m fra startpunktet (59.9230926, 10.755531)
        oppdaterMed(NvdbBomstasjon(id = 1L, lat = 59.9231, lng = 10.7555, navn = null, takstLitenBilRush = null))

        Assertions.assertThat(bomstasjonService.harBomstasjonPåRute(osloPolyline)).isTrue()
        verify(exactly = 1) { nvdbBomstasjonClient.hentAlleBomstasjoner() }
    }

    @Test
    fun `harBomvei returnerer false når bomstasjon er langt fra ruten`() {
        // Bergen — hundrevis av km unna
        oppdaterMed(NvdbBomstasjon(id = 2L, lat = 60.418, lng = 5.313, navn = null, takstLitenBilRush = null))

        Assertions.assertThat(bomstasjonService.harBomstasjonPåRute(osloPolyline)).isFalse()
        verify(exactly = 1) { nvdbBomstasjonClient.hentAlleBomstasjoner() }
    }

    @Test
    fun `harBomvei returnerer false med tom bomstasjonsliste`() {
        oppdaterMed()

        Assertions.assertThat(bomstasjonService.harBomstasjonPåRute(osloPolyline)).isFalse()
        verify(exactly = 1) { nvdbBomstasjonClient.hentAlleBomstasjoner() }
    }

    private fun oppdaterMed(vararg stasjoner: NvdbBomstasjon) {
        every { nvdbBomstasjonClient.hentAlleBomstasjoner() } returns stasjoner.toList()
    }
}
