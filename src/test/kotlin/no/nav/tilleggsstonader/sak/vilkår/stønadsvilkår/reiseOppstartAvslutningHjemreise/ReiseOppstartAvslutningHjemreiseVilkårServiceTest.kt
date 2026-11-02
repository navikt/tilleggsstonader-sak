package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.AdvisoryLockService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiExceptionHandler
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.lagreReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaMidlertidigBostedPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.Optional

class ReiseOppstartAvslutningHjemreiseVilkårServiceTest {
    @Test
    fun `HTTP deserialisering avviser manglende adresser uten å lekke requesten`() {
        val service = mockk<ReiseOppstartAvslutningHjemreiseVilkårService>()
        val controller = ReiseOppstartAvslutningHjemreiseVilkårController(mockk(relaxed = true), service)
        val mvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(ApiExceptionHandler())
                .setMessageConverters(JacksonJsonHttpMessageConverter(jsonMapper))
                .build()
        val fakta =
            mapOf(
                "type" to "MIDLERTIDIG_BOSTED_PERIODE",
                "aktivitetId" to VilkårperiodeGlobalId.random().toString(),
                "midlertidigAdresse" to "Kun-i-request 1",
                "hjemstedAdresse" to "Kun-i-request 2",
            )
        val request = mapOf("fom" to "2026-01-05", "tom" to "2026-01-15", "svar" to emptyMap<String, Any>(), "fakta" to fakta)
        val ugyldige =
            listOf(request - "tom") +
                listOf("midlertidigAdresse", "hjemstedAdresse").flatMap { felt ->
                    listOf(
                        request + ("fakta" to fakta - felt),
                        request + ("fakta" to fakta + (felt to "")),
                        request + ("fakta" to fakta + (felt to " \n\t")),
                        request + ("fakta" to fakta + (felt to null)),
                    )
                }
        ugyldige.forEach {
            val response =
                mvc
                    .perform(
                        post("/api/vilkar/reise-oppstart-avslutning-hjemreise/${BehandlingId.random()}")
                            .contentType(APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(it)),
                    ).andExpect(status().isBadRequest())
                    .andReturn()
                    .response
            assertThat(response.contentAsString).doesNotContain("Kun-i-request", "2026-01")
        }
        verify(exactly = 0) { service.opprettNyttVilkår(any(), any()) }
    }

    @Test
    fun `regelstruktur inkluderer midlertidig bostedsperiode`() {
        val controller = ReiseOppstartAvslutningHjemreiseVilkårController(mockk(), mockk())
        assertFileJsonIsEqual("vilkår/regelstruktur/REISE_OPPSTART_AVSLUTNING_HJEMREISE.json", controller.regler())
    }

    @Nested
    inner class MidlertidigeBostedsperioder {
        private val repository = mockk<VilkårRepository>()
        private val behandlingService = mockk<BehandlingService>()
        private val periodeService = mockk<VilkårperiodeService>()
        private val unleash = mockk<UnleashService>()
        private val lock = mockk<AdvisoryLockService>()
        private val fellesService = VilkårService(behandlingService, repository, mockk())
        private val service =
            ReiseOppstartAvslutningHjemreiseVilkårService(repository, behandlingService, fellesService, periodeService, unleash, lock)
        private val behandling =
            saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO), steg = StegType.VILKÅR)
        private val aktivitet =
            aktivitet(behandlingId = behandling.id, fom = 1 januar 2026, tom = 31 januar 2026)
                .withTypeOrThrow<AktivitetFaktaOgVurdering>()
        private val request =
            LagreVilkårReiseOppstartAvslutningHjemreise(
                fom = 5 januar 2026,
                tom = 15 januar 2026,
                fakta = FaktaMidlertidigBostedPeriode(aktivitet.globalId, "Testveien 1", "Testveien 2"),
                svar =
                    listOf(
                        RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE,
                        RegelId.ROAH_HAR_BARN_UNDER_18_ÅR_SOM_FLYTTER_MED,
                        RegelId.ROAH_HAR_BARN_IKKE_FULLFØRT_FJERDE_SKOLEÅR_HJEMME,
                        RegelId.ROAH_HAR_DOKUMENTERTE_SÆRSKILTE_BEHOV_FLERE_HJEMREISER,
                    ).associateWith { SvarOgBegrunnelse(SvarId.JA, null) },
            )
        private val lagrede = mutableMapOf<VilkårId, Vilkår>()
        private var låst = false

        @BeforeEach
        fun setup() {
            every { behandlingService.hentSaksbehandling(behandling.id) } answers {
                assertThat(låst).isTrue()
                behandling
            }
            every { periodeService.hentAktivitet(aktivitet.globalId, behandling.id) } returns aktivitet
            every { unleash.isEnabled(Toggle.KAN_BEHANDLE_REISE_OPPSTART_AVSLUTNING_HJEMREISE) } returns true
            every { lock.lockForTransaction<Any>("roah-midlertidig-bosted:${behandling.id}", any()) } answers {
                låst = true
                try {
                    secondArg<() -> Any>().invoke()
                } finally {
                    låst = false
                }
            }
            every { repository.findByBehandlingId(behandling.id) } answers { lagrede.values.filter { it.behandlingId == behandling.id } }
            every { repository.findById(any()) } answers {
                assertThat(låst).isTrue()
                Optional.ofNullable(lagrede[firstArg()])
            }
            every { repository.insert(any<Vilkår>()) } answers {
                assertThat(låst).isTrue()
                firstArg<Vilkår>().also { lagrede[it.id] = it }
            }
            every { repository.update(any<Vilkår>()) } answers {
                assertThat(låst).isTrue()
                firstArg<Vilkår>().also { lagrede[it.id] = it }
            }
            every { repository.deleteById(any()) } answers { lagrede.remove(firstArg<VilkårId>()) }
        }

        @ParameterizedTest
        @EnumSource(Stønadstype::class, names = ["REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO", "REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR"])
        fun `oppretter og oppdaterer på begge stønader`(stønadstype: Stønadstype) {
            every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling.copy(stønadstype = stønadstype)
            val opprettet = service.opprettNyttVilkår(request, behandling.id)
            assertThat(opprettet.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
            assertThat(opprettet.status).isEqualTo(VilkårStatus.NY)
            assertThat(opprettet.delvilkårsett.single().vurderinger).hasSize(4)
            assertThat(opprettet.fakta).isEqualTo(request.fakta.mapTilVilkårFakta())
            val oppdatert = service.oppdaterVilkår(request.copy(tom = 16 januar 2026), behandling.id, opprettet.id)
            assertThat(opprettet.id).isEqualTo(oppdatert.id)
            assertThat(oppdatert.tom).isEqualTo(16 januar 2026)
            assertThat(oppdatert.status).isEqualTo(VilkårStatus.NY)
            val nyeFakta = bostedsfakta().copy(midlertidigAdresse = "Testveien 3", hjemstedAdresse = "Testveien 4")
            val adresseEndret = service.oppdaterVilkår(request.copy(fakta = nyeFakta), behandling.id, opprettet.id)
            assertThat(adresseEndret.fakta).isEqualTo(nyeFakta.mapTilVilkårFakta())
            assertThat(adresseEndret.tilDto().fakta.mapTilFakta()).isEqualTo(nyeFakta)
        }

        @Test
        fun `avslag krever begrunnelse og ufullstendig kjede er ikke vurdert`() {
            listOf(
                Pair(
                    mapOf(RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE to SvarOgBegrunnelse(SvarId.NEI, "Ikke nødvendig")),
                    Vilkårsresultat.IKKE_OPPFYLT,
                ),
                Pair(
                    mapOf(RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE to SvarOgBegrunnelse(SvarId.JA, null)),
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                ),
            ).forEach { (svar, resultat) ->
                lagrede.clear()
                assertThat(service.opprettNyttVilkår(request.copy(svar = svar), behandling.id).resultat).isEqualTo(resultat)
            }
        }

        @Test
        fun `manglende påkrevd begrunnelse avvises ved oppretting og oppdatering`() {
            val ugyldigeRequester =
                listOf(null, "", " \n\t").map { begrunnelse ->
                    request.copy(svar = mapOf(RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE to SvarOgBegrunnelse(SvarId.NEI, begrunnelse)))
                }
            ugyldigeRequester.forEach { ugyldigRequest ->
                assertThatThrownBy {
                    service.opprettNyttVilkår(ugyldigRequest, behandling.id)
                }.isInstanceOf(Feil::class.java)
                    .hasMessageContaining("Påkrevd begrunnelse")
            }
            val opprettet = service.opprettNyttVilkår(request, behandling.id)
            ugyldigeRequester.forEach { ugyldigRequest ->
                assertThatThrownBy {
                    service.oppdaterVilkår(ugyldigRequest, behandling.id, opprettet.id)
                }.isInstanceOf(Feil::class.java)
                    .hasMessageContaining("Påkrevd begrunnelse")
            }
        }

        @Test
        fun `inklusive grenser og ufullstendige perioder reserverer intervallet`() {
            val eksisterende = service.opprettNyttVilkår(request.copy(svar = emptyMap()), behandling.id)
            listOf(
                request,
                request.copy(fom = 15 januar 2026, tom = 20 januar 2026),
                request.copy(fom = 1 januar 2026, tom = 5 januar 2026),
            ).forEach {
                assertThatThrownBy { service.opprettNyttVilkår(it, behandling.id) }
                    .isInstanceOf(ApiFeil::class.java)
                    .hasMessageContaining("kan ikke overlappe")
            }
            val neste = service.opprettNyttVilkår(request.copy(fom = 16 januar 2026, tom = 20 januar 2026), behandling.id)
            assertThatThrownBy {
                service.oppdaterVilkår(request, behandling.id, neste.id)
            }.isInstanceOf(ApiFeil::class.java)
            service.oppdaterVilkår(request, behandling.id, eksisterende.id)
            assertThat(lagrede).hasSize(2)
        }

        @Test
        fun `ulike aktiviteter og slettede perioder kan overlappe`() {
            val eksisterende = service.opprettNyttVilkår(request, behandling.id)
            val annenAktivitet = aktivitet.copy(globalId = VilkårperiodeGlobalId.random())
            every { periodeService.hentAktivitet(annenAktivitet.globalId, behandling.id) } returns annenAktivitet
            service.opprettNyttVilkår(request.copy(fakta = bostedsfakta().copy(aktivitetId = annenAktivitet.globalId)), behandling.id)
            lagrede[eksisterende.id] = eksisterende.markerSlettet("Ikke lenger aktuell")
            service.opprettNyttVilkår(request, behandling.id)
            assertThat(lagrede).hasSize(3)
        }

        @Test
        fun `bytte av aktivitet validerer overlapp på ny aktivitet`() {
            val første = service.opprettNyttVilkår(request, behandling.id)
            val annenAktivitet = aktivitet.copy(globalId = VilkårperiodeGlobalId.random())
            every { periodeService.hentAktivitet(annenAktivitet.globalId, behandling.id) } returns annenAktivitet
            val annenRequest = request.copy(fakta = bostedsfakta().copy(aktivitetId = annenAktivitet.globalId))
            service.opprettNyttVilkår(annenRequest, behandling.id)
            assertThatThrownBy { service.oppdaterVilkår(annenRequest, behandling.id, første.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("kan ikke overlappe")
            assertThat(lagrede[første.id]).isEqualTo(første)
            val flyttet =
                service.oppdaterVilkår(
                    annenRequest.copy(fom = 16 januar 2026, tom = 20 januar 2026),
                    behandling.id,
                    første.id,
                )
            assertThat(flyttet.fakta).isEqualTo(annenRequest.fakta.mapTilVilkårFakta())
        }

        @Test
        fun `perioder på en annen behandling blokkerer ikke lagring`() {
            val periode = service.opprettNyttVilkår(request, behandling.id)
            lagrede[periode.id] = periode.copy(behandlingId = BehandlingId.random())
            val ny = service.opprettNyttVilkår(request, behandling.id)
            assertThat(ny.behandlingId).isEqualTo(behandling.id)
            assertThat(lagrede).hasSize(2)
        }

        @Test
        fun `validerer datoer og oppfylt aktivitet på riktig behandling`() {
            assertThatThrownBy { request.copy(fom = 16 januar 2026) }.isInstanceOf(ApiFeil::class.java)
            listOf(request.copy(fom = request.fom.minusMonths(1)), request.copy(tom = request.tom.plusMonths(1))).forEach {
                assertThatThrownBy { service.opprettNyttVilkår(it, behandling.id) }
                    .isInstanceOf(ApiFeil::class.java)
                    .hasMessageContaining("hele vilkårperioden")
            }
            every { periodeService.hentAktivitet(aktivitet.globalId, behandling.id) } returns
                aktivitet.copy(resultat = ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThatThrownBy { service.opprettNyttVilkår(request, behandling.id) }.hasMessageContaining("ikke oppfylt")
            every { periodeService.hentAktivitet(aktivitet.globalId, behandling.id) } returns null
            assertThatThrownBy { service.opprettNyttVilkår(request, behandling.id) }.hasMessageContaining("finnes ikke")
            verify(exactly = 0) { repository.insert(any<Vilkår>()) }
        }

        @Test
        fun `avviser toggle av feil stønadstype låst behandling og feil steg`() {
            every { unleash.isEnabled(any<Toggle>()) } returns false
            assertThatThrownBy { service.opprettNyttVilkår(request, behandling.id) }.isInstanceOf(Feil::class.java)
            every { unleash.isEnabled(any<Toggle>()) } returns true
            listOf(
                behandling.copy(stønadstype = Stønadstype.BARNETILSYN),
                behandling.copy(steg = StegType.BEREGNE_YTELSE),
                behandling.copy(status = BehandlingStatus.FERDIGSTILT),
            ).forEach {
                every { behandlingService.hentSaksbehandling(behandling.id) } returns it
                assertThatThrownBy {
                    service.opprettNyttVilkår(
                        request,
                        behandling.id,
                    )
                }.isInstanceOf(RuntimeException::class.java)
            }
            verify(exactly = 0) { repository.insert(any<Vilkår>()) }
        }

        @Test
        fun `bevarer opphav og sporbarhet og endrer uendret status`() {
            val opprettet = service.opprettNyttVilkår(request, behandling.id)
            val kopiert =
                opprettet.copy(
                    status = VilkårStatus.UENDRET,
                    opphavsvilkår = Opphavsvilkår(BehandlingId.random(), opprettet.sporbar.opprettetTid),
                )
            lagrede[kopiert.id] = kopiert
            val oppdatert = service.oppdaterVilkår(request, behandling.id, kopiert.id)
            assertThat(oppdatert.status).isEqualTo(VilkårStatus.ENDRET)
            assertThat(oppdatert.opphavsvilkår).isEqualTo(kopiert.opphavsvilkår)
            assertThat(oppdatert.sporbar).isEqualTo(kopiert.sporbar)
            assertThat(service.oppdaterVilkår(request, behandling.id, kopiert.id).status).isEqualTo(VilkårStatus.ENDRET)
        }

        @Test
        fun `sletter nye permanent og eldre med kommentar uten å endre opphav`() {
            val ny = service.opprettNyttVilkår(request, behandling.id)
            assertThat(service.slettVilkår(behandling.id, ny.id, null).slettetPermanent).isTrue()
            assertThat(lagrede).isEmpty()
            val eldre =
                ny.copy(
                    status = VilkårStatus.UENDRET,
                    opphavsvilkår = Opphavsvilkår(BehandlingId.random(), ny.sporbar.opprettetTid),
                )
            lagrede[eldre.id] = eldre
            assertThatThrownBy {
                service.slettVilkår(behandling.id, eldre.id, null)
            }.isInstanceOf(IllegalArgumentException::class.java)
            val slettet = service.slettVilkår(behandling.id, eldre.id, "Perioden gjelder ikke")
            assertThat(slettet.slettetPermanent).isFalse()
            assertThat(slettet.vilkår.status).isEqualTo(VilkårStatus.SLETTET)
            assertThat(slettet.vilkår.opphavsvilkår).isEqualTo(eldre.opphavsvilkår)
            assertThatThrownBy {
                service.oppdaterVilkår(
                    request,
                    behandling.id,
                    eldre.id,
                )
            }.hasMessageContaining("allerede slettet")
            assertThatThrownBy {
                service.slettVilkår(
                    behandling.id,
                    eldre.id,
                    "Igjen",
                )
            }.hasMessageContaining("allerede slettet")
        }

        @Test
        fun `oppdatering og sletting avviser annen behandling eller type før mutasjon`() {
            val ny = service.opprettNyttVilkår(request, behandling.id)
            listOf(ny.copy(behandlingId = BehandlingId.random()), ny.copy(type = VilkårType.DAGLIG_REISE)).forEach {
                lagrede[ny.id] = it
                assertThatThrownBy {
                    service.oppdaterVilkår(
                        request,
                        behandling.id,
                        ny.id,
                    )
                }.isInstanceOf(ApiFeil::class.java)
                assertThatThrownBy { service.slettVilkår(behandling.id, ny.id, null) }.isInstanceOf(ApiFeil::class.java)
                assertThat(lagrede[ny.id]).isEqualTo(it)
            }
            verify(exactly = 0) {
                repository.update(any<Vilkår>())
                repository.deleteById(any())
            }
        }

        @Test
        fun `avviser bytte mellom reise og bostedsperiode uten lagring`() {
            val ny = service.opprettNyttVilkår(request, behandling.id)
            val reise =
                lagreReiseOppstartAvslutningHjemreiseDto(aktivitetId = aktivitet.globalId).tilDomene().copy(
                    fom = request.fom,
                    tom = request.tom,
                )
            assertThatThrownBy { service.oppdaterVilkår(reise, behandling.id, ny.id) }.isInstanceOf(ApiFeil::class.java)
            assertThat(lagrede[ny.id]).isEqualTo(ny)
            val lagretReise = service.opprettNyttVilkår(reise, behandling.id)
            assertThatThrownBy { service.oppdaterVilkår(request, behandling.id, lagretReise.id) }.isInstanceOf(ApiFeil::class.java)
            assertThat(lagrede[lagretReise.id]).isEqualTo(lagretReise)
            verify(exactly = 0) { repository.update(any<Vilkår>()) }
        }

        @Test
        fun `JSON round trip beholder egen faktatype uten reisefelter`() {
            val fakta: VilkårFakta = request.fakta.mapTilVilkårFakta()
            val json = jsonMapper.writeValueAsString(fakta)
            assertThat(json).doesNotContain("reiseId", "typeReiseformål")
            assertThat(jsonMapper.readValue(json, VilkårFakta::class.java)).isEqualTo(fakta)
        }

        @Test
        fun `blandet henting skiller reiser og perioder og bevarer slettede perioder`() {
            val periode = service.opprettNyttVilkår(request, behandling.id)
            lagrede[periode.id] = periode.markerSlettet("Feilregistrert")
            val reise =
                service.opprettNyttVilkår(
                    lagreReiseOppstartAvslutningHjemreiseDto(aktivitetId = aktivitet.globalId)
                        .tilDomene()
                        .copy(fom = request.fom, tom = request.tom),
                    behandling.id,
                )
            every { periodeService.hentVilkårperioder(behandling.id) } returns
                Vilkårperioder(aktiviteter = listOf(aktivitet), målgrupper = emptyList())
            val hentet = service.hentVilkårGruppertPåAktivitet(behandling.id).single().tilDto()
            assertThat(hentet.reiser.map { it.id }).containsExactly(reise.id)
            assertThat(hentet.midlertidigeBostedsperioder.single().status).isEqualTo(VilkårStatus.SLETTET)
            assertThat(hentet.midlertidigeBostedsperioder.single().slettetKommentar).isEqualTo("Feilregistrert")
            assertThat(service.hentVilkårForBehandling(behandling.id).map { it.id }).containsExactly(reise.id)
        }

        private fun bostedsfakta() = request.fakta as FaktaMidlertidigBostedPeriode

        @Test
        fun `felles lås og copy bevarer reisehistorikk ved formålsendring`() {
            val reise =
                lagreReiseOppstartAvslutningHjemreiseDto(aktivitetId = aktivitet.globalId)
                    .tilDomene()
                    .copy(fom = request.fom, tom = request.fom)
            val ny = service.opprettNyttVilkår(reise, behandling.id)
            val kopiert =
                ny.copy(
                    status = VilkårStatus.UENDRET,
                    opphavsvilkår = Opphavsvilkår(BehandlingId.random(), ny.sporbar.opprettetTid),
                )
            lagrede[ny.id] = kopiert
            val nyeFakta =
                (reise.fakta as FaktaOffentligTransport)
                    .copy(typeReiseformål = TypeReiseformål.AVSLUTNING)
            val endret = service.oppdaterVilkår(reise.copy(fakta = nyeFakta), behandling.id, ny.id)
            assertThat(endret.id).isEqualTo(kopiert.id)
            assertThat(endret.behandlingId).isEqualTo(kopiert.behandlingId)
            assertThat(endret.opphavsvilkår).isEqualTo(kopiert.opphavsvilkår)
            assertThat(endret.sporbar).isEqualTo(kopiert.sporbar)
            assertThat(endret.status).isEqualTo(VilkårStatus.ENDRET)
            assertThat(endret.fakta).isEqualTo(nyeFakta.mapTilVilkårFakta())
            listOf(
                FaktaPrivatBil(
                    reiseId = nyeFakta.reiseId,
                    aktivitetId = nyeFakta.aktivitetId,
                    typeReiseformål = nyeFakta.typeReiseformål,
                    nødvendigUtgift = nyeFakta.nødvendigUtgift,
                    reiseavstand = 10.toBigDecimal(),
                ),
                FaktaUbestemtType(nyeFakta.reiseId, nyeFakta.aktivitetId, nyeFakta.typeReiseformål),
            ).forEach { fakta ->
                val transportEndret = service.oppdaterVilkår(reise.copy(fakta = fakta), behandling.id, ny.id)
                assertThat(transportEndret.fakta).isEqualTo(fakta.mapTilVilkårFakta())
                assertThat(transportEndret.opphavsvilkår).isEqualTo(kopiert.opphavsvilkår)
                assertThat(transportEndret.sporbar).isEqualTo(kopiert.sporbar)
                assertThat(transportEndret.status).isEqualTo(VilkårStatus.ENDRET)
            }
            val slettet = service.slettVilkår(behandling.id, endret.id, "Reisen utgår")
            assertThat(slettet.vilkår.status).isEqualTo(VilkårStatus.SLETTET)
            assertThat(slettet.vilkår.opphavsvilkår).isEqualTo(kopiert.opphavsvilkår)
            assertThatThrownBy { service.oppdaterVilkår(reise, behandling.id, ny.id) }.hasMessageContaining("allerede slettet")
            assertThatThrownBy { service.slettVilkår(behandling.id, ny.id, "Igjen") }.hasMessageContaining("allerede slettet")
        }
    }

    @Test
    fun `skal sette tom lik fom for oppstart når tom ikke er satt`() {
        val fom = 10 januar 2026

        val domain =
            lagreReiseOppstartAvslutningHjemreiseDto(
                fom = fom,
                tom = null,
                typeReiseformål = TypeReiseformål.OPPSTART,
            ).tilDomene()

        assertThat(domain.fom).isEqualTo(fom)
        assertThat(domain.tom).isEqualTo(fom)
    }

    @Test
    fun `skal feile om tom ikke er lik fom for avslutning`() {
        assertThatThrownBy {
            lagreReiseOppstartAvslutningHjemreiseDto(
                fom = 10 januar 2026,
                tom = 11 januar 2026,
                typeReiseformål = TypeReiseformål.AVSLUTNING,
            ).tilDomene()
        }.isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Tom kan ikke settes til noe annet enn lik fom")
    }

    @Test
    fun `skal feile når tom mangler for hjemreise`() {
        assertThatThrownBy {
            lagreReiseOppstartAvslutningHjemreiseDto(
                fom = 10 januar 2026,
                tom = null,
                typeReiseformål = TypeReiseformål.HJEMREISE,
            ).tilDomene()
        }.isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("tom må settes for hjemreise")
    }
}
