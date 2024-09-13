package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.dto.tilDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.AktivitetService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val aktivitetService: AktivitetService,
    private val fagsakService: FagsakService,
) {

    val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Hent alle behandlinger som er iverksatt og sjekk om det er stønadsperioder som må kontrolleres.
     *
     * Hent aktiviteter og sjekk om det er aktiviteter som overlapper stønadsperiodene.
     *
     * Hent ytelsesperioder og gjør det samme
     *
     * Ignorer aktivitetestypene, og målgruppene som vi ikke henter. (Reel arbeidssøker)
     *
     */
    fun hentBehandlingerForOppfølging(): List<BehandlingForOppfølgingDto> {
        val behandlingerMedMuligLøpendePerioder = behandlingRepository.finnGjeldendeIverksatteBehandlinger()
        return behandlingerMedMuligLøpendePerioder.mapNotNull { behandling ->
            val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

            val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandling.id)

            val registerAktivitet = aktivitetService.hentAktiviteter(
                fagsak.fagsakPersonId,
                stønadsperioder.first().fom,
                stønadsperioder.last().tom,
            )
            val stønadsperioderSomMåSjekkes =
                stønadsperioder.filterNot { stønadsperiodeMåKontrolleres(it, fagsak, registerAktivitet) }

            if (stønadsperioderSomMåSjekkes.isNotEmpty()) {
                BehandlingForOppfølgingDto(
                    behandling.tilDto(fagsak.stønadstype, fagsak.fagsakPersonId),
                    stønadsperioderSomMåSjekkes,
                )
            } else {
                null
            }
        }
    }

    fun stønadsperiodeMåKontrolleres(
        stønadsperiode: StønadsperiodeDto,
        fagsak: Fagsak,
        registerAktivitet: List<AktivitetArenaDto>,
    ): Boolean {
        fun perioderErSammenhengende(
            a: ArenaAktivitetPeriode,
            b: ArenaAktivitetPeriode,
        ) = a.tom.plusDays(1) == b.fom

        return when (stønadsperiode.aktivitet) {
            AktivitetType.REELL_ARBEIDSSØKER -> false
            AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
            AktivitetType.TILTAK -> {
                val tiltak = registerAktivitet
                    .asSequence()
                    .filter { tiltakHarRelevantStatus(it) }
                    .filterNot { tiltakErUtdanning(it) }
                    .mapNotNull { mapTilPeriode(it) }
                    .filter { it.overlapper(stønadsperiode) }
                    .sortedBy { it.fom }
                    .toList()
                    .mergeSammenhengende { a, b -> a.overlapper(b) || perioderErSammenhengende(a, b) }

                val stønadsperiodeHarOverlappendeTiltak = tiltak.any { it.inneholder(stønadsperiode) }
                !stønadsperiodeHarOverlappendeTiltak
            }
            AktivitetType.UTDANNING -> {
                val utdanning = registerAktivitet
                    .asSequence()
                    .filter { tiltakHarRelevantStatus(it) }
                    .filter { tiltakErUtdanning(it) }
                    .mapNotNull { mapTilPeriode(it) }
                    .filter { it.overlapper(stønadsperiode) }
                    .sortedBy { it.fom }
                    .toList()
                    .mergeSammenhengende { a, b -> a.overlapper(b) || perioderErSammenhengende(a, b) }

                val stønadsperiodeHarOverlappendeUtdanning =
                    utdanning.any { it.fom <= stønadsperiode.fom && it.tom >= stønadsperiode.tom }
                !stønadsperiodeHarOverlappendeUtdanning
            }
        }
    }

    private fun mapTilPeriode(aktivitet: AktivitetArenaDto): ArenaAktivitetPeriode? {
        if (aktivitet.fom == null || aktivitet.tom == null) {
            logger.warn("Aktivitet med id=${aktivitet.id} mangler fom eller tom dato: ${aktivitet.fom} - ${aktivitet.tom}")
            return null
        }
        return ArenaAktivitetPeriode(aktivitet.fom!!, aktivitet.tom!!)
    }

    private fun tiltakHarRelevantStatus(it: AktivitetArenaDto) =
        it.status != StatusAktivitet.AVBRUTT && it.status != StatusAktivitet.OPPHØRT

    private fun tiltakErUtdanning(it: AktivitetArenaDto) = it.erUtdanning ?: false
}

data class ArenaAktivitetPeriode(override val fom: LocalDate, override val tom: LocalDate) :
    Periode<LocalDate>,
    Mergeable<LocalDate, ArenaAktivitetPeriode> {
    override fun merge(other: ArenaAktivitetPeriode): ArenaAktivitetPeriode {
        return ArenaAktivitetPeriode(minOf(fom, other.fom), maxOf(tom, other.tom))
    }
}
