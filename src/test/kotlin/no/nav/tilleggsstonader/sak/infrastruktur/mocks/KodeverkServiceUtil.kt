package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkService

object KodeverkServiceUtil {
    fun mockedKodeverkService() =
        mockk<KodeverkService>().apply {
            val service = this
            every { service.hentLandkode(any()) } returns "Norge"
            every { service.hentLandkode("SWE") } returns "Sverige"
            every { service.hentLandkode("FIN") } returns "Finland"

            every { service.hentPoststed(any()) } returns "Oslo"
        }
}
