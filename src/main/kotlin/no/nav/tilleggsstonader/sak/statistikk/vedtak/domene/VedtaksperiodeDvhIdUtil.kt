package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * TODO: skall - ikke tatt i bruk ennå.
 *
 * For BARNETILSYN og LÆREMIDLER finnes det ingen naturlig 1:1-kobling mellom en persistert
 * [no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode] og periodene som bygges opp av
 * beregningsresultatet (se [VedtaksperiodePassAvBarnMapper]/[VedtaksperiodeLæremidlerMapper] og
 * [VedtaksperioderDvh.id]).
 *
 * Denne utility-klassen skal brukes til å generere en deterministisk [VedtaksperiodeId] for disse
 * sammenslåtte periodene, slik at samme periode alltid får samme id ved reprosessering
 * (f.eks. [no.nav.tilleggsstonader.sak.statistikk.vedtak.OppdaterVedtaksstatistikkTask]).
 *
 * Denne id-en er kun ment for kobling internt mellom [VedtaksperioderDvh] og [UtbetalingerDvh],
 * og representerer IKKE en persistert [no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode].
 *
 * TODO: Når denne tas i bruk må [AndelTilVedtaksperiodeIdKobler] for BARNETILSYN og LÆREMIDLER
 *  oppdateres til å matche andeler mot de sammenslåtte periodene (med denne id-en) i stedet for
 *  mot `vedtak.vedtaksperioder`, slik at [UtbetalingerDvh.vedtaksperiodeIder] og [VedtaksperioderDvh.id]
 *  refererer til samme id-sett.
 */
object VedtaksperiodeDvhIdUtil {
    /**
     * TODO: skall - verifiser at [periodeEgenskaper] inneholder alle feltene som er med på å
     *  definere en unik periode for stønadstypen (fom, tom, målgruppe, aktivitet, studienivå/antallBarn osv.),
     *  slik at to reelt ulike perioder aldri kan kollidere.
     */
    fun genererDeterministiskId(
        behandlingId: BehandlingId,
        vararg periodeEgenskaper: Any?,
    ): VedtaksperiodeId {
        val nøkkel = (listOf(behandlingId) + periodeEgenskaper.toList()).joinToString("|")
        val uuid = UUID.nameUUIDFromBytes(nøkkel.toByteArray(StandardCharsets.UTF_8))
        return VedtaksperiodeId(uuid)
    }
}
