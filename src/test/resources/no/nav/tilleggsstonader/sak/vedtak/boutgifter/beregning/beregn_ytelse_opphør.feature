# language: no
# encoding: UTF-8

Egenskap: Opphør av boutgifter

  Scenario: Skal klippe utbetalingsperioden fra revurder fra-datoen

    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.03.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så kan vi forvente følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 01.01.2025      |
      | 03.02.2025 | 1000  | BOUTGIFTER_AAP | 01.02.2025      |
      | 03.03.2025 | 1000  | BOUTGIFTER_AAP | 01.03.2025      |
    # Merk: 03.02.25 og 03.03.25 tilsvarer første ukedag i hhv februar og mars 2025

    Når kopierer perioder fra forrige boutgiftbehandling for behandling=2

    Når vi opphører boutgifter behandling=2 med revurderFra=15.02.2025

    Så kan vi forvente følgende beregningsresultat for behandling=2
      | Fom        | Tom        | Beløp | Maks sats | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 1000  | 4953      | 01.01.2025      | Ja                          |
      | 01.02.2025 | 14.02.2025 | 1000  | 4953      | 01.02.2025      | Ja                          |

    Så kan vi forvente følgende andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 01.01.2025      |
      | 03.02.2025 | 1000  | BOUTGIFTER_AAP | 01.02.2025      |

    Så kan vi forvente følgende vedtaksperioder for behandling=2
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 14.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |