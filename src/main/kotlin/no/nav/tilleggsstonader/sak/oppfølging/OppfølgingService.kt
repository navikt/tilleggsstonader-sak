package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.dto.tilDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.AktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val aktivitetService: AktivitetService,
    private val fagsakService: FagsakService,
    private val ytelseService: YtelseService,
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
<<<<<<< Updated upstream
            val registerAktivitet = aktivitetService.hentAktiviteter(fagsak.fagsakPersonId, fom, tom)
            val sammenslåtteAktiviteter = slåSammenAktiviteter(registerAktivitet)
=======
            val registeraktiviteter = aktivitetService.hentAktiviteter(fagsak.fagsakPersonId, fom = fom, tom = tom)
            val ytelser = ytelseService.hentYtelseForGrunnlag(behandling.id, fom = fom, tom = tom)

>>>>>>> Stashed changes
            val stønadsperioderSomMåSjekkes =
                stønadsperioder.filter { stønadsperiodeMåKontrolleres(it, fagsak, registeraktiviteter) }
                    .map { PeriodeTilKontroll(it, årsak = mutableSetOf(ÅrsakKontroll.AKTIVITET)) }
                    .associateBy { it.stønadsperiodeDto.id!! }

            val sammenslåtteAktiviteter = slåSammenYtelser(ytelser)
            val sammenslåtteYtelser = slåSammenYtelser(ytelser)

            stønadsperioder
                .filter { stønadsperiodeMåKontrolleres(it, fagsak, ytelser) }

            if (stønadsperioderSomMåSjekkes.isNotEmpty()) {
                BehandlingForOppfølgingDto(
                    behandling.tilDto(fagsak.stønadstype, fagsak.fagsakPersonId),
                    stønadsperioderSomMåSjekkes.values.toList(),
                    mapFeiledeYtelser(ytelser)
                )
            } else {
                null
            }
        }
    }

<<<<<<< Updated upstream
    private fun slåSammenAktiviteter(registerAktivitet: List<AktivitetArenaDto>) =
        registerAktivitet
            .mapNotNull { mapTilPeriode(it) }
            .sorted()
            .mergeSammenhengende { a, b ->
                (a.aktivitet.erUtdanning ?: false) == (b.aktivitet.erUtdanning ?: false) &&
                        a.overlapper(b) || perioderErSammenhengende(a, b)
            }
=======
    private fun slåSammenYtelser(ytelser: YtelsePerioderDto) =
        ytelser.perioder.map { HolderYtelse(it) }
            .sorted()
            .mergeSammenhengende { a, b -> a.ytelse == b.ytelse && a.overlapper(b) || perioderErSammenhengende(a, b) }

    private fun mapFeiledeYtelser(ytelser: YtelsePerioderDto): List<TypeYtelsePeriode> {
        return ytelser.hentetInformasjon
            .filter { it.status != StatusHentetInformasjon.FEILET }
            .map { it.type }
    }
>>>>>>> Stashed changes

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

<<<<<<< Updated upstream
=======
    fun stønadsperiodeMåKontrolleres(
        stønadsperiode: StønadsperiodeDto,
        fagsak: Fagsak,
        ytelser: YtelsePerioderDto,
    ): Boolean {
        return when (stønadsperiode.målgruppe) {

            MålgruppeType.AAP -> finnOverlappendePerioder()
            MålgruppeType.OVERGANGSSTØNAD -> TODO()

            MålgruppeType.OMSTILLINGSSTØNAD,
            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE,
            MålgruppeType.DAGPENGER -> false
        }
    }

    private fun perioderErSammenhengende(a: Periode<LocalDate>, b: Periode<LocalDate>) =
        a.tom.plusDays(1) == b.fom

>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
data class AktivitetHolder(val aktivitet: AktivitetArenaDto) :
=======
private data class HolderYtelse(
    val ytelse: YtelsePeriode
): Periode<LocalDate>, Mergeable<LocalDate, HolderYtelse> {
    override val fom: LocalDate get() = ytelse.fom
    override val tom: LocalDate get() = ytelse.tom ?: LocalDate.MAX
    override fun merge(other: HolderYtelse): HolderYtelse {
        return this.copy(ytelse = this.ytelse.copy(fom = minOf(fom, other.fom), tom = maxOf(tom, other.tom)))
    }
}


data class ArenaAktivitetPeriode(override val fom: LocalDate, override val tom: LocalDate) :
>>>>>>> Stashed changes
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
