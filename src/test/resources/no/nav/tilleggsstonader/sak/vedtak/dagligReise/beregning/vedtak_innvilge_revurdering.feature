# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av daglig reise - revurdering

  Bakgrunn:
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 01.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |


  Scenario: revurdering av invilget førstegangsbehandlingq
  Resultat: revurderer for den nye vedtaksperioden

    Gitt følgende beregningsinput for offentlig transport behandling=1
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 | 44                 | 778                       | 1                         |

    Når beregner for daglig reise offentlig transport behandling=1

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Enkeltbillett-antall |
      | 01.01.2025 | 30.01.2025 | 440   | 10                   |

    Når vi kopierer perioder fra forrige daglig reise behandling for behandling=2

    Når vi innvilger daglig reise behandling=2 med tidligsteEndring=31.01.2025
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 31.01.2025 | 01.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=1
      | Fom        | Tom        | Beløp | Enkeltbillett-antall |
      | 01.01.2025 | 30.01.2025 | 440   | 10                   |

    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=2
      | Fom        | Tom        | Beløp | Enkeltbillett-antall |
      | 31.01.2025 | 01.03.2025 | 440   | 10                   |







