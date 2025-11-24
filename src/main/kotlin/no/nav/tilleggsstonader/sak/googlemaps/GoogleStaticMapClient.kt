package no.nav.tilleggsstonader.sak.googlemaps

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleStaticMapClient(
    builder: RestClient.Builder,
) {
    private val apiKey = "nøkkel"

    private val baseUrl =
        URI(
            "https://maps.googleapis.com/maps/api/staticmap",
        )

    private val restClient = builder.baseUrl(baseUrl.toString()).build()

    fun hentStaticMap(polyline: String): ByteArray? {
        val polyline2 =
            """sfzlJuiw`Ay@yA]pAo@~B]jAc@bBGTEBCHg@{@Wa@{AeCO[i@gAWm@gC}FWi@uAaDsAwDQ_@?OACCCA_@SsDCm@h@Sp@AtDaBzBuAHUbEkBhDoArCgAn@Ur@[d@SjBkAj@a@fCoCnAgBHOv@yABEHQd@kABIVo@HWDUd@}AZgAh@iBbAqDt@oCnAiFl@iCh@kC`A{GzA}KLyAHe@pB_OLs@j@wCj@yBHUz@}Bz@eB@CLQnAgBxAqAtAw@xAa@vAS`AIdBGb@@pEDp@@P?~@Gj@INEjA_@p@e@X[j@o@@ChAyBx@iBJWb@aArBaFb@}@LWVk@lA}AbA}@n@a@DCtAg@dAOlFm@|@M|B]LC\ERArBWfAUf@O`@Sx@g@XULBz@w@x@uAn@qAXW`@IZHb@f@FJr@jBh@rAf@vAJNPL|BbGv@lBL`@xA~DvAvD~@fCRf@R^^hAf@jBRhANfAXfB\dC^vBb@~Bj@|B`@fBd@tBd@xBLd@RbA^|AVjALj@H\h@|Bj@hBLX`@r@`@b@RNLFTD\Bl@@hAq@h@[r@q@bAw@|@_@t@Gv@Nl@d@j@v@b@hA^dBTbCRtF@VZhMRxC\fCb@dBj@|AjAjBf@t@^b@b@d@JJDFf@j@Z\tBdCzBfCr@x@VXHJz@pAHLPZJRj@jAj@pAn@fBRj@t@jCPl@XhAP|@VnA\vB\rCbApJtAtNl@rFfBnPLpBHfA@`@JdDExDSbEYvC]`C_@nBqAdEiAvCYf@}A`BeCrA]Nw@^w@^SNWR]Xq@n@aAz@s@x@cB`CgA~B[t@gAvCw@|CIXaAzGgCbTSxBS|BCXCZ_@bJMXIvDMt@]p@k@TQAYSq@}@{B}C_@a@ERk@fDi@g@cD_DEEkBgBECMKg@e@eDyC"""
        val uriString = "https://maps.googleapis.com/maps/api/staticmap?size=600x300&path=enc:$polyline2&key=$apiKey"

        val uri =
            UriComponentsBuilder
                .fromUriString(uriString)
                .build(false) // Kan ikke encode pga av polyline fra GoogleMaps som ikke kan endre noen karakterer
                .toUri()

        return restClient
            .get()
            .uri(uri)
            .retrieve()
            .body(ByteArray::class.java)
    }
}
