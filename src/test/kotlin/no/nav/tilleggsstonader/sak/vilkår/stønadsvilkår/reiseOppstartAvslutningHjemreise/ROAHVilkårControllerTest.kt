package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ROAHTestUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaMidlertidigBostedPeriodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsrDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ROAHVilkårControllerTest : CleanDatabaseIntegrationTest() {
    val ident = "12345678910"

    @Autowired
    private lateinit var service: ReiseOppstartAvslutningHjemreiseVilkårService

    @Autowired
    private lateinit var repository: VilkårRepository

    @Autowired
    private lateinit var foreslåVedtaksperiodeService: ForeslåVedtaksperiodeService

    @ParameterizedTest
    @EnumSource(Stønadstype::class, names = ["REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO", "REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR"])
    fun `CRUD henter bostedsperioder separat fra reiser og bevarer historikk`(stønadstype: Stønadstype) {
        val (behandlingId, aktivitetId) = opprettROAHBehandling(stønadstype)
        val klient = Testklient(this)
        val base = "/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId"
        val request = bostedsperiode(aktivitetId)
        val ny = klient.post(base, request).expectOkWithBody<VilkårReiseOppstartAvslutningHjemreiseDto>()
        assertThat(ny.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(ny.status).isEqualTo(VilkårStatus.NY)
        assertThat(ny.fakta).isEqualTo(request.fakta)
        assertThat(ny.delvilkårsett.single().vurderinger).hasSize(4)
        val reise =
            kall.vilkårReiseOppstartAvslutningHjemreise.opprettVilkår(
                behandlingId,
                LagreVilkårReiseOppstartAvslutningHjemreiseDto(
                    fom = 1 januar 2026,
                    tom = null,
                    svar = ROAHTestUtil.oppfylteSvarOppstartAvslutningOffentligTransportDto(),
                    fakta = offentligTransport(100.toBigDecimal(), aktivitetId),
                ),
            )
        val hentet = kall.vilkårReiseOppstartAvslutningHjemreise.hentVilkår(behandlingId).single()
        assertThat(hentet.reiser.map { it.id }).containsExactly(reise.id)
        assertThat(hentet.midlertidigeBostedsperioder).containsExactly(ny)
        val forslag = foreslåVedtaksperiodeService.foreslåPerioder(behandlingId)
        assertThat(forslag.map { it.fom to it.tom }).containsExactly((1 januar 2026) to (1 januar 2026))
        val oppdatert =
            klient
                .put("$base/${ny.id}", request.copy(tom = 20 januar 2026))
                .expectOkWithBody<VilkårReiseOppstartAvslutningHjemreiseDto>()
        assertThat(oppdatert.tom).isEqualTo(20 januar 2026)
        assertThat(oppdatert.status).isEqualTo(VilkårStatus.NY)
        klient.put("$base/${reise.id}", request).expectStatus().isBadRequest()
        assertThat(repository.findByIdOrThrow(reise.id).type).isEqualTo(VilkårType.ROAH_OPPSTART_AVSLUTNING)
        val slettet =
            klient
                .delete("$base/${ny.id}", SlettVilkårRequestDto())
                .expectOkWithBody<SlettVilkårResultatDto>()
        assertThat(slettet.slettetPermanent).isTrue()
        assertThat(
            kall.vilkårReiseOppstartAvslutningHjemreise
                .hentVilkår(behandlingId)
                .single()
                .midlertidigeBostedsperioder,
        ).isEmpty()

        val eldre = klient.post(base, request).expectOkWithBody<VilkårReiseOppstartAvslutningHjemreiseDto>()
        val lagret = repository.findByIdOrThrow(eldre.id)
        val opphav = Opphavsvilkår(behandlingId, lagret.sporbar.opprettetTid)
        repository.update(lagret.copy(status = VilkårStatus.UENDRET, opphavsvilkår = opphav))
        val endret =
            klient
                .put("$base/${eldre.id}", request)
                .expectOkWithBody<VilkårReiseOppstartAvslutningHjemreiseDto>()
        assertThat(endret.status).isEqualTo(VilkårStatus.ENDRET)
        assertThat(repository.findByIdOrThrow(eldre.id).opphavsvilkår).isEqualTo(opphav)
        val markert =
            klient
                .delete("$base/${eldre.id}", SlettVilkårRequestDto("Ikke lenger aktuell"))
                .expectOkWithBody<SlettVilkårResultatDto>()
        assertThat(markert.slettetPermanent).isFalse()
        assertThat(markert.vilkår.status).isEqualTo(VilkårStatus.SLETTET)
        assertThat(markert.vilkår.slettetKommentar).isEqualTo("Ikke lenger aktuell")
        klient.put("$base/${eldre.id}", request).expectStatus().isBadRequest()
        klient.delete("$base/${eldre.id}", SlettVilkårRequestDto()).expectStatus().isBadRequest()
        assertThat(
            kall.vilkårReiseOppstartAvslutningHjemreise
                .hentVilkår(behandlingId)
                .single()
                .midlertidigeBostedsperioder,
        ).containsExactly(markert.vilkår)
    }

    @Test
    fun `manglende blanke adresser og tom gir 400 uten innsendte verdier`() {
        val (behandlingId, aktivitetId) = opprettROAHBehandling()
        val klient = Testklient(this)
        val base = "/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId"
        val fakta =
            mapOf(
                "type" to "MIDLERTIDIG_BOSTED_PERIODE",
                "aktivitetId" to aktivitetId.toString(),
                "midlertidigAdresse" to "Kun-i-request 1",
                "hjemstedAdresse" to "Kun-i-request 2",
            )
        val request =
            mapOf(
                "fom" to "2026-01-05",
                "tom" to "2026-01-15",
                "svar" to emptyMap<String, Any>(),
                "fakta" to fakta,
            )
        val ugyldige =
            listOf(
                request - "tom",
                request + ("tom" to null),
            ) +
                listOf("midlertidigAdresse", "hjemstedAdresse").flatMap { felt ->
                    listOf(
                        request + ("fakta" to fakta - felt),
                        request + ("fakta" to fakta + (felt to null)),
                        request + ("fakta" to fakta + (felt to "")),
                        request + ("fakta" to fakta + (felt to " \n\t")),
                    )
                }
        ugyldige.forEach {
            klient
                .post(base, it)
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .value<String> { melding ->
                    assertThat(melding).doesNotContain("Kun-i-request", "2026-01")
                }
        }
        val ny = klient.post(base, bostedsperiode(aktivitetId)).expectOkWithBody<VilkårReiseOppstartAvslutningHjemreiseDto>()
        val før = repository.findByIdOrThrow(ny.id)
        ugyldige.forEach {
            klient
                .put("$base/${ny.id}", it)
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.detail")
                .value<String> { melding ->
                    assertThat(melding).doesNotContain("Kun-i-request", "2026-01")
                }
        }
        assertThat(repository.findByIdOrThrow(ny.id)).isEqualTo(før)
    }

    @Test
    fun `generiske ruter og annen behandling kan ikke omgå periodelagringen`() {
        val (behandlingId, aktivitetId) = opprettROAHBehandling()
        val (annenBehandlingId, _) = opprettROAHBehandling(Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR)
        val klient = Testklient(this)
        val request = bostedsperiode(aktivitetId)
        val base = "/api/vilkar/reise-oppstart-avslutning-hjemreise"
        val ny = klient.post("$base/$behandlingId", request).expectOkWithBody<VilkårReiseOppstartAvslutningHjemreiseDto>()
        val før = repository.findByIdOrThrow(ny.id)
        klient.put("$base/$annenBehandlingId/${ny.id}", request).expectStatus().isBadRequest()
        klient
            .delete(
                "$base/$annenBehandlingId/${ny.id}",
                SlettVilkårRequestDto(),
            ).expectStatus()
            .isBadRequest()
        klient.post("$base/$annenBehandlingId", request).expectStatus().isBadRequest()
        klient
            .post(
                "/api/vilkar/opprett",
                OpprettVilkårDto(
                    vilkårType = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
                    behandlingId = behandlingId,
                    delvilkårsett = ny.delvilkårsett,
                    fom = ny.fom,
                    tom = ny.tom,
                    utgift = null,
                    erFremtidigUtgift = false,
                    offentligTransport = null,
                ),
            ).expectStatus()
            .isBadRequest()
        klient
            .post(
                "/api/vilkar",
                SvarPåVilkårDto(
                    id = ny.id,
                    behandlingId = behandlingId,
                    delvilkårsett = ny.delvilkårsett,
                    fom = ny.fom,
                    tom = ny.tom,
                    utgift = null,
                    erFremtidigUtgift = false,
                    offentligTransport = null,
                ),
            ).expectStatus()
            .isBadRequest()
        klient.post("/api/vilkar/ikkevurder", OppdaterVilkårDto(ny.id, behandlingId)).expectStatus().isBadRequest()
        klient.delete("/api/vilkar", SlettVilkårRequest(ny.id, behandlingId)).expectStatus().isBadRequest()
        assertThat(repository.findByIdOrThrow(ny.id)).isEqualTo(før)
    }

    @Test
    fun `samtidige overlappende innsettinger lagrer kun én periode med PostgreSQL transaksjonslås`() {
        val (behandlingId, aktivitetId) = opprettROAHBehandling()
        val request = bostedsperiode(aktivitetId)
        val klar = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val oppgaver =
                (1..2).map {
                    executor.submit<Result<Vilkår>> {
                        klar.countDown()
                        check(start.await(10, TimeUnit.SECONDS))
                        runCatching { testWithBrukerContext { service.opprettNyttVilkår(request.tilDomene(), behandlingId) } }
                    }
                }
            check(klar.await(10, TimeUnit.SECONDS))
            start.countDown()
            val resultater = oppgaver.map { it.get(30, TimeUnit.SECONDS) }
            assertThat(resultater.count { it.isSuccess }).isEqualTo(1)
            assertThat(resultater.single { it.isFailure }.exceptionOrNull()).isInstanceOf(ApiFeil::class.java)
            assertThat(
                repository.findByBehandlingId(behandlingId).filter { it.type == VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE },
            ).hasSize(1)
        } finally {
            start.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    private fun opprettROAHBehandling(
        stønadstype: Stønadstype = Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
    ): Pair<BehandlingId, VilkårperiodeGlobalId> {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = stønadstype,
                identer = setOf(PersonIdent(ident)),
            )
        opprettOgTilordneOppgaveForBehandling(behandling.id)
        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id, tilSteg = StegType.VILKÅR) {
            aktivitet {
                opprett {
                    if (stønadstype == Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO) {
                        aktivitetTiltakTsoReiseTilOppstart(1 januar 2026, 31 mars 2026)
                    } else {
                        add {
                            lagreVilkårperiodeAktivitet(
                                behandlingId = it,
                                fom = 1 januar 2026,
                                tom = 31 mars 2026,
                                faktaOgSvar = FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsrDto,
                            )
                        }
                    }
                }
            }
            målgruppe {
                opprett {
                    if (stønadstype == Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO) {
                        målgruppeAAP(1 januar 2026, 31 mars 2026)
                    } else {
                        målgruppeDagpenger(1 januar 2026, 31 mars 2026)
                    }
                }
            }
        }
        return behandling.id to
            kall.vilkårperiode
                .hentForBehandling(behandling.id)
                .vilkårperioder.aktiviteter
                .single()
                .globalId
    }

    private fun bostedsperiode(aktivitetId: VilkårperiodeGlobalId) =
        LagreVilkårReiseOppstartAvslutningHjemreiseDto(
            fom = 5 januar 2026,
            tom = 15 januar 2026,
            fakta = FaktaMidlertidigBostedPeriodeDto(aktivitetId, "Testveien 1", "Testveien 2"),
            svar =
                listOf(
                    RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE,
                    RegelId.ROAH_HAR_BARN_UNDER_18_ÅR_SOM_FLYTTER_MED,
                    RegelId.ROAH_HAR_BARN_IKKE_FULLFØRT_FJERDE_SKOLEÅR_HJEMME,
                    RegelId.ROAH_HAR_DOKUMENTERTE_SÆRSKILTE_BEHOV_FLERE_HJEMREISER,
                ).associateWith { SvarOgBegrunnelseDto(SvarId.JA, null) },
        )

    @Test
    fun `skal hente alle regler som tilhører reise oppstart avslutning og hjemreise`() {
        val regelstruktur = kall.vilkårReiseOppstartAvslutningHjemreise.regler()

        assertFileJsonIsEqual("vilkår/regelstruktur/REISE_OPPSTART_AVSLUTNING_HJEMREISE.json", regelstruktur)
    }

    @ParameterizedTest
    @EnumSource(Stønadstype::class, names = ["REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO", "REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR"])
    fun `skal kunne lagre endre og slette reise med offentlig transport`(stønadstype: Stønadstype) {
        val fom = 1 januar 2026
        val (behandlingId, aktivitetId) = opprettROAHBehandling(stønadstype)

        val nyttVilkår =
            LagreVilkårReiseOppstartAvslutningHjemreiseDto(
                fom = fom,
                tom = null,
                svar = ROAHTestUtil.oppfylteSvarOppstartAvslutningOffentligTransportDto(),
                fakta = offentligTransport(BigDecimal.valueOf(100), aktivitetId),
            )

        val opprettetVilkår = kall.vilkårReiseOppstartAvslutningHjemreise.opprettVilkår(behandlingId, nyttVilkår)

        assertThat(opprettetVilkår.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(opprettetVilkår.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(lagreVilkårRequest = nyttVilkår, resultat = opprettetVilkår, forventetTom = nyttVilkår.fom)

        val oppdatertVilkår =
            nyttVilkår.copy(
                fakta =
                    offentligTransport(BigDecimal.valueOf(150), aktivitetId),
            )

        val resultatOppdatert =
            kall.vilkårReiseOppstartAvslutningHjemreise.oppdaterVilkår(
                lagreVilkår = oppdatertVilkår,
                vilkårId = opprettetVilkår.id,
                behandlingId = behandlingId,
            )

        assertThat(resultatOppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultatOppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert, forventetTom = oppdatertVilkår.fom)

        val resultatSlettet =
            kall.vilkårReiseOppstartAvslutningHjemreise.slettVilkår(
                behandlingId = behandlingId,
                vilkårId = resultatOppdatert.id,
                dto = SlettVilkårRequestDto(),
            )

        assertThat(resultatSlettet.slettetPermanent).isTrue
        assertThat(resultatSlettet.vilkår.slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårReiseOppstartAvslutningHjemreise.hentVilkår(behandlingId)
        assertThat(hentedeVilkår.single().reiser).isEmpty()
    }

    private fun assertLagretVilkår(
        lagreVilkårRequest: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
        resultat: VilkårReiseOppstartAvslutningHjemreiseDto,
        forventetTom: LocalDate? = lagreVilkårRequest.tom,
    ) {
        assertThat(resultat.fom).isEqualTo(lagreVilkårRequest.fom)
        assertThat(resultat.tom).isEqualTo(forventetTom)
        assertThat(resultat.fakta).isEqualTo(lagreVilkårRequest.fakta)
        assertThat(resultat.delvilkårsett).hasSize(1)

        assertAlleSvarHarFåttVurdering(delvilkår = resultat.delvilkårsett, svar = lagreVilkårRequest.svar)
    }

    private fun assertAlleSvarHarFåttVurdering(
        delvilkår: List<DelvilkårDto>,
        svar: Map<RegelId, SvarOgBegrunnelseDto>,
    ) {
        val brukteRegelIder = delvilkår.flatMap { it.vurderinger.map { vurdering -> vurdering.regelId } }.toSet()

        assertThat(brukteRegelIder).hasSize(svar.size)
    }

    private fun offentligTransport(
        utgift: BigDecimal,
        aktivitetId: VilkårperiodeGlobalId,
    ) = FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
        reiseId = dummyReiseId,
        aktivitetId = aktivitetId,
        typeReiseformål = TypeReiseformål.OPPSTART,
        begrunnelse = "buss",
        nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.JA, begrunnelse = null),
        utgifterOffentligTransport = utgift,
    )
}
