package no.nav.tilleggsstonader.sak.opplysninger.arena

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.arena.VedtakStatus
import java.time.LocalDate

object ArenaStatusDtoUtil {

    fun arenaStatusDto(
        vedtakStatus: VedtakStatus = vedtakStatus(),
        sakStatus: SakStatus = SakStatus(false),
    ) = ArenaStatusDto(
        sak = sakStatus,
        vedtak = vedtakStatus,
    )

    fun vedtakStatus(
        harVedtak: Boolean = false,
        harAktivtVedtak: Boolean = false,
        harVedtakUtenUtfall: Boolean = false,
        vedtakTom: LocalDate? = null,
    ) = VedtakStatus(
        harVedtak = harVedtak,
        harAktivtVedtak = harAktivtVedtak,
        harVedtakUtenUtfall = harVedtakUtenUtfall,
        vedtakTom = vedtakTom,
    )
}
