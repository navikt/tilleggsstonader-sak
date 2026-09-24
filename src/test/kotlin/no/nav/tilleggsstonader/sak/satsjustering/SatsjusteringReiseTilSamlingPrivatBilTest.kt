package no.nav.tilleggsstonader.sak.satsjustering

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.vedtak.sats.satser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SatsjusteringReiseTilSamlingPrivatBilTest(
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : IntegrationTest() {
    // Perioden ligger etter siste bekreftede sats, uavhengig av når nye satser blir bekreftet
    private val sisteBekreftedeÅr = satser.filter { it.bekreftet }.maxOf { it.fom.year }
    private val fom = 1 januar (sisteBekreftedeÅr + 1)
    private val tom = 31 januar (sisteBekreftedeÅr + 1)

    @Test
    fun `andel for privatbil-reise skal vente på satsendring når satsen ikke er bekreftet`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        privatBilReiseTilSamling(fom, tom)
                    }
                }
            }

        val andeler =
            tilkjentYtelseRepository
                .findByBehandlingId(behandlingContext.behandlingId)!!
                .andelerTilkjentYtelse

        assertThat(andeler).hasSize(1)
        assertThat(andeler.single().statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
    }
}
