package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.oppsummering.tilOppsummertVilkår
import no.nav.tilleggsstonader.sak.util.dummyAktivitetId
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.lagreReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaMidlertidigBostedPeriodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreisePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.ReiseFaktaROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.tilReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class FaktaReiseOppstartAvslutningHjemreiseTest {
    @Test
    fun `felles request response og sletterespons roundtrip for alle faktavarianter`() {
        val fakta =
            listOf(
                lagreReiseOppstartAvslutningHjemreiseDto().fakta,
                FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
                    reiseId = dummyReiseId,
                    aktivitetId = dummyAktivitetId,
                    typeReiseformål = TypeReiseformål.HJEMREISE,
                    nødvendigUtgift = NødvendigUtgiftDto(SvarJaNei.JA, null),
                    reiseavstand = BigDecimal.TEN,
                ),
                FaktaReiseOppstartAvslutningHjemreiseUbestemtDto(dummyReiseId, dummyAktivitetId, TypeReiseformål.AVSLUTNING),
                FaktaMidlertidigBostedPeriodeDto(dummyAktivitetId, "Testveien 1", "Testveien 2"),
            )
        fakta.forEach { variant ->
            val request =
                LagreVilkårReiseOppstartAvslutningHjemreiseDto(
                    fom = 1 januar 2026,
                    tom = 1 januar 2026,
                    svar = emptyMap(),
                    fakta = variant,
                )
            val json = jsonMapper.writeValueAsString(request)
            assertThat(jsonMapper.readTree(json).propertyNames()).containsExactlyInAnyOrder("fom", "tom", "svar", "fakta")
            assertThat(jsonMapper.readValue(json, LagreVilkårReiseOppstartAvslutningHjemreiseDto::class.java)).isEqualTo(request)
            assertThat(jsonMapper.readValue(json, LagreVilkårReiseOppstartAvslutningHjemreiseDto::class.java).tilDomene())
                .isEqualTo(request.tilDomene())
            val lagret =
                vilkår(
                    type =
                        when (variant) {
                            is FaktaMidlertidigBostedPeriodeDto -> VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE
                            is ReiseFaktaROAHDto -> variant.typeReiseformål.tilVilkårType()
                        },
                    fom = request.fom,
                    tom = request.tom,
                    fakta = variant.mapTilFakta().mapTilVilkårFakta(),
                )
            val response = lagret.tilDto()
            val responseJson = jsonMapper.writeValueAsString(response)
            assertThat(jsonMapper.readTree(responseJson).propertyNames()).doesNotContain("reiseId", "aktivitetId", "typeReiseformål")
            assertThat(jsonMapper.readValue(responseJson, VilkårReiseOppstartAvslutningHjemreiseDto::class.java)).isEqualTo(response)
            assertThat(response.fakta).isEqualTo(variant)
            val slettet = SlettetVilkårResultat(slettetPermanent = true, vilkår = lagret).tilReiseOppstartAvslutningHjemreiseDto()
            assertThat(jsonMapper.readValue(jsonMapper.writeValueAsString(slettet), SlettVilkårResultatDto::class.java)).isEqualTo(slettet)
        }
    }

    @Test
    fun `leser gamle persisted reisefakta og skriver samme JSON uten API-typeverdier`() {
        val felles = """"reiseId":"$dummyReiseId","aktivitetId":"$dummyAktivitetId","typeReiseformål":"OPPSTART""""
        val nødvendig = """"nødvendigUtgift":{"erUtgiftenNødvendig":"JA","begrunnelse":null}"""
        val varianter =
            mapOf(
                "OFFENTLIG_TRANSPORT" to """$nødvendig,"utgifterOffentligTransport":40,"begrunnelse":null""",
                "PRIVAT_BIL" to
                    """$nødvendig,"reiseavstand":40,"bompenger":null,"fergekostnad":null,"parkering":null,"piggdekkavgift":null,"begrunnelse":null""",
                "UBESTEMT" to "",
            )
        varianter.forEach { (type, felter) ->
            val json = """{"type":"REISE_OPPSTART_AVSLUTNING_HJEMREISE_$type",$felles${if (felter.isEmpty()) "" else ",$felter"}}"""
            val fakta = jsonMapper.readValue(json, VilkårFakta::class.java)
            val lagret = vilkår(type = VilkårType.ROAH_OPPSTART_AVSLUTNING, fom = 1 januar 2026, tom = 1 januar 2026, fakta = fakta)
            val domene = lagret.tilDto().fakta.mapTilFakta()
            assertThat(domene.mapTilVilkårFakta()).isEqualTo(fakta)
            assertThat(jsonMapper.readTree(jsonMapper.writeValueAsString(domene.mapTilVilkårFakta()))).isEqualTo(jsonMapper.readTree(json))
        }
    }

    @Test
    fun `bostedsadresser maskeres og finnes ikke i oppsummering eller reisefaktakontrakter`() {
        val fakta = FaktaMidlertidigBostedPeriodeDto(dummyAktivitetId, "Testveien 1", "Testveien 2")
        val domene = fakta.mapTilFakta()
        val persisted = domene.mapTilVilkårFakta()
        listOf(fakta, domene, persisted).forEach {
            assertThat(it.toString()).doesNotContain("Testveien")
        }
        assertThat(persisted).isNotInstanceOf(ReiseVilkårFakta::class.java).isNotInstanceOf(FaktaROAHReise::class.java)
        val lagret = vilkår(type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE, fom = 1 januar 2026, tom = 2 januar 2026, fakta = persisted)
        assertThat(lagret.toString()).doesNotContain("Testveien")
        assertThat(lagret.tilDto().toString()).doesNotContain("Testveien")
        assertThat(jsonMapper.writeValueAsString(lagret.tilOppsummertVilkår())).doesNotContain("Testveien", "Adresse")
        assertThat(jsonMapper.readValue(jsonMapper.writeValueAsString(persisted), VilkårFakta::class.java)).isEqualTo(persisted)
    }

    @Test
    fun `blanke adresser og manglende tom avvises med faste brukerfeil`() {
        val fakta = FaktaMidlertidigBostedPeriodeDto(dummyAktivitetId, "Testveien 1", "Testveien 2")
        listOf("", " ", "\n\t").forEach { blank ->
            val midlertidigFeil = assertThrows<ApiFeil> { fakta.copy(midlertidigAdresse = blank).mapTilFakta() }
            assertThat(midlertidigFeil.message).isEqualTo("Midlertidig adresse må fylles ut")
            val hjemstedFeil = assertThrows<ApiFeil> { fakta.copy(hjemstedAdresse = blank).mapTilFakta() }
            assertThat(hjemstedFeil.message).isEqualTo("Hjemstedsadresse må fylles ut")
        }
        val request = LagreVilkårReiseOppstartAvslutningHjemreiseDto(1 januar 2026, null, emptyMap(), fakta)
        assertThat(assertThrows<ApiFeil> { request.tilDomene() }.message).isEqualTo("tom må settes for midlertidig bostedsperiode")
    }

    @Test
    fun `skal mappe nødvendig utgift for offentlig transport`() {
        val fakta =
            FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
                reiseId = dummyReiseId,
                aktivitetId = dummyAktivitetId,
                typeReiseformål = TypeReiseformål.OPPSTART,
                nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.JA, begrunnelse = null),
                utgifterOffentligTransport = 40.toBigDecimal(),
            ).mapTilFakta()

        assertThat(fakta.nødvendigUtgift.erUtgiftenNødvendig).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `skal mappe nødvendig utgift for privat bil`() {
        val fakta =
            FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
                reiseId = dummyReiseId,
                aktivitetId = dummyAktivitetId,
                typeReiseformål = TypeReiseformål.OPPSTART,
                nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.NEI, begrunnelse = "Ikke nødvendig"),
                reiseavstand = 40.toBigDecimal(),
                begrunnelse = null,
            ).mapTilFakta()

        assertThat(fakta.nødvendigUtgift.erUtgiftenNødvendig).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `skal kreve begrunnelse når nødvendig utgift er NEI`() {
        val feil =
            assertThrows<ApiFeil> {
                FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
                    reiseId = dummyReiseId,
                    aktivitetId = dummyAktivitetId,
                    typeReiseformål = TypeReiseformål.OPPSTART,
                    nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.NEI, begrunnelse = " "),
                    utgifterOffentligTransport = 40.toBigDecimal(),
                ).mapTilFakta()
            }

        assertThat(feil.message).isEqualTo("Begrunnelse må fylles ut når utgift ikke vurderes som nødvendig")
    }

    @Test
    fun `skal avvise GAMMEL_MANGLER_DATA i nødvendig utgift`() {
        val feil =
            assertThrows<Feil> {
                FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
                    reiseId = dummyReiseId,
                    aktivitetId = dummyAktivitetId,
                    typeReiseformål = TypeReiseformål.OPPSTART,
                    nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.GAMMEL_MANGLER_DATA, begrunnelse = null),
                    reiseavstand = BigDecimal.TEN,
                ).mapTilFakta()
            }

        assertThat(feil.message).isEqualTo("erUtgiftenNødvendig må være JA eller NEI")
    }
}
