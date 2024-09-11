package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.dto.tilDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.AktivitetService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val aktivitetService: AktivitetService,
    private val fagsakService: FagsakService,
) {

    fun hentBehandlingerForOppfølging(): List<BehandlingForOppfølgingDto> {
        val behandlingerMedMuligLøpendePerioder = behandlingRepository.finnGjeldendeIverksatteBehandlinger()

        /**
         * Hent aktiviteter og sjekk om det finnes "opphør" i perioder som har blitt brukt. Dvs hvis man har en stønadsperiode med Tiltak 01.05-31.05 og man ikke finner noe tiltak for overlapper med denne perioden, så vil det kunne påvirke vedtaksperioden.
         *
         * Hvis det er type reell arbeidsssøker så trenger man ikke å verifisere noe?
         *
         * Henter ytelsesperioder og gjør det samme
         *
         * Hvis det er en type ytelse(målgruppe) som vi ikke henter, så kan man ignore den?
         *
         */
        return behandlingerMedMuligLøpendePerioder.mapNotNull { behandling ->
            val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

            val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandling.id)

            val registerAktivitet = aktivitetService.hentAktiviteter(
                fagsak.fagsakPersonId,
                stønadsperioder.first().fom,
                stønadsperioder.last().tom
            )
            val stønadsperioderSomMåSjekkes =
                stønadsperioder.filterNot { stønadsperiodeMåKontrolleres(it, fagsak, registerAktivitet) }

            if (stønadsperioderSomMåSjekkes.isNotEmpty()) {
                BehandlingForOppfølgingDto(
                    behandling.tilDto(fagsak.stønadstype, fagsak.fagsakPersonId),
                    stønadsperioderSomMåSjekkes
                )
            } else {
                null
            }
        }
    }

    fun stønadsperiodeMåKontrolleres(
        stønadsperiode: StønadsperiodeDto,
        fagsak: Fagsak,
        registerAktivitet: List<AktivitetArenaDto>
    ): Boolean {
        return when (stønadsperiode.aktivitet) {
            AktivitetType.REELL_ARBEIDSSØKER -> {
                false
            }

            AktivitetType.INGEN_AKTIVITET -> {
                error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
            }

            AktivitetType.TILTAK -> {
                val tiltak = registerAktivitet
                    .filter { it.tom!! >= stønadsperiode.fom && it.fom!! <= stønadsperiode.tom }
                    .filterNot { it.erUtdanning ?: false || it.status == StatusAktivitet.AVBRUTT || it.status == StatusAktivitet.OPPHØRT }
                    .sortedBy { it.fom }
                    .slåSammenSammenhengende()

                val stønadsperiodeHarOverlappendeTiltak =
                    tiltak.any { it.fom <= stønadsperiode.fom && it.tom >= stønadsperiode.tom }
                !stønadsperiodeHarOverlappendeTiltak
            }

            AktivitetType.UTDANNING -> {
                val utdanning = registerAktivitet
                    .filter { it.tom!! >= stønadsperiode.fom && it.fom!! <= stønadsperiode.tom }
                    .filter { it.erUtdanning ?: false && it.status != StatusAktivitet.AVBRUTT && it.status != StatusAktivitet.OPPHØRT }
                    .sortedBy { it.fom }
                    .slåSammenSammenhengende()

                val stønadsperiodeHarOverlappendeUtdanning =
                    utdanning.any { it.fom <= stønadsperiode.fom && it.tom >= stønadsperiode.tom }
                !stønadsperiodeHarOverlappendeUtdanning
            }
        }
    }
}

data class ArenaAktivitetPeriode(val fom: LocalDate, val tom: LocalDate)

private fun List<AktivitetArenaDto>.slåSammenSammenhengende(): List<ArenaAktivitetPeriode> =
    this.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()

        if (last == null) {
            acc.add(ArenaAktivitetPeriode(entry.fom!!, entry.tom!!))
        } else {
            // Hvis last og entry overlapper eller er sammenhengende: slå sammen til en periode
            val overlapper = !(last.tom < entry.fom || last.fom > entry.tom)
            val erSammenhengende = last.tom.plusDays(1) == entry.fom

            if (overlapper || erSammenhengende) {
                acc.removeLast()
                acc.add(ArenaAktivitetPeriode(minOf(last.fom, entry.fom!!), maxOf(last.tom, entry.tom!!)))
            } else {
                acc.add(ArenaAktivitetPeriode(entry.fom!!, entry.tom!!))
            }
        }

        acc
    }
