# language: no
# encoding: UTF-8

Egenskap: Beregning av faste utgifter en bolig - revurdering

  Scenario: Skal ta med alle perioder som starter etter revuderFra
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_EN_BOLIG
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når beregner boutgifter med revurderFra=2025-01-01

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Skal ikke ta med perioder som slutter før revurderFra
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 01.03.2025 | 31.03.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_EN_BOLIG
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |
      | 01.03.2025 | 31.03.2025 | 5000   |

    Når beregner boutgifter med revurderFra=2025-03-01

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.03.2025 | 31.03.2025 | 1              | 4953         | 4953      | 01.03.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |



