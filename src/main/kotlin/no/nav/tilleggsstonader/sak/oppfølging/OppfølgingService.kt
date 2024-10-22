package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
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

            val fom = stønadsperioder.minOf { it.fom }
            val tom = stønadsperioder.maxOf { it.tom }
            val registerAktivitet = aktivitetService.hentAktiviteter(fagsak.fagsakPersonId, fom, tom)
            val sammenslåtteAktiviteter = slåSammenAktiviteter(registerAktivitet)
            val stønadsperioderSomMåSjekkes =
                stønadsperioder.filter { stønadsperiodeMåKontrolleres(it, fagsak, registerAktivitet) }

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

    private fun slåSammenAktiviteter(registerAktivitet: List<AktivitetArenaDto>) =
        registerAktivitet
            .mapNotNull { mapTilPeriode(it) }
            .sorted()
            .mergeSammenhengende { a, b ->
                (a.aktivitet.erUtdanning ?: false) == (b.aktivitet.erUtdanning ?: false) &&
                        a.overlapper(b) || perioderErSammenhengende(a, b)
            }

    fun stønadsperiodeMåKontrolleres(
        stønadsperiode: StønadsperiodeDto,
        fagsak: Fagsak,
        registerAktivitet: List<AktivitetArenaDto>,
    ): Boolean {
        return when (stønadsperiode.aktivitet) {
            AktivitetType.REELL_ARBEIDSSØKER -> false
            AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
            AktivitetType.TILTAK -> {
                val tiltak = finnOverlappendePerioder(registerAktivitet.filterNot(::tiltakErUtdanning), stønadsperiode)

                tiltak.none { it.inneholder(stønadsperiode) }
            }

            AktivitetType.UTDANNING -> {
                val utdanning =
                    finnOverlappendePerioder(registerAktivitet.filter { tiltakErUtdanning(it) }, stønadsperiode)

                utdanning.none { it.inneholder(stønadsperiode) }
            }
        }
    }

    private fun finnOverlappendePerioder(
        registeraktiviteter: List<AktivitetArenaDto>,
        stønadsperiode: StønadsperiodeDto,
    ) = registeraktiviteter
        .asSequence()
        .filter { tiltakHarRelevantStatus(it) }
        .mapNotNull { mapTilPeriode(it) }
        .filter { it.overlapper(stønadsperiode) }
        .sorted()
        .toList()
        .mergeSammenhengende { a, b -> a.overlapper(b) || perioderErSammenhengende(a, b) }

    private fun mapTilPeriode(aktivitet: AktivitetArenaDto): AktivitetHolder? {
        if (aktivitet.fom == null || aktivitet.tom == null) {
            logger.warn("Aktivitet med id=${aktivitet.id} mangler fom eller tom dato: ${aktivitet.fom} - ${aktivitet.tom}")
            return null
        }
        return AktivitetHolder(aktivitet)
    }

    // TODO Usikker på denne
    private fun tiltakHarRelevantStatus(it: AktivitetArenaDto) =
        true
    // it.status != StatusAktivitet.AVBRUTT && it.status != StatusAktivitet.OPPHØRT

    private fun tiltakErUtdanning(it: AktivitetArenaDto) = it.erUtdanning ?: false
}

data class AktivitetHolder(val aktivitet: AktivitetArenaDto) :
    Periode<LocalDate>,
    Mergeable<LocalDate, AktivitetHolder> {

    override val fom: LocalDate get() = aktivitet.fom!!
    override val tom: LocalDate get() = aktivitet.tom!!

    override fun merge(other: AktivitetHolder): AktivitetHolder {
        return AktivitetHolder(
            aktivitet = this.aktivitet.copy(
                fom = minOf(fom, other.fom),
                tom = maxOf(tom, other.tom)
            )
        )
    }
}

private fun perioderErSammenhengende(
    a: Periode<LocalDate>,
    b: Periode<LocalDate>,
) = a.tom.plusDays(1) == b.fom
