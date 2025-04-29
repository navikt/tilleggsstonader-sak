# language: no
# encoding: UTF-8

Egenskap: Beregning - med revurderFra

  Scenario: Skal ta med alle perioder som starter etter revurderFra
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |

    Når beregner boutgifter med revurderFra=2025-01-07

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 07.01.2025 | 09.01.2025 | 1              | 1000         | 4953      | 07.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |


  Scenario: Skal ikke ta med perioder som slutter før revurderFra - to ulike utbetalingsperioder
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 16.02.2025 | 19.02.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |
      | 16.02.2025 | 19.02.2025 | 1000   |

    Når beregner boutgifter med revurderFra=2025-02-16

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 16.02.2025 | 19.02.2025 | 1              | 1000         | 4953      | 16.02.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Skal ta med perioder fra den utbetalingsperioden man revurder dersom de overlapper
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 20.01.2025 | 25.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |
      | 20.01.2025 | 25.01.2025 | 1000   |

    Når beregner boutgifter med revurderFra=2025-01-22

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 07.01.2025 | 25.01.2025 | 1              | 2000         | 4953      | 07.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Skal ta med perioder fra den utbetalingsperioden man revurder dersom de overlapper - beløpet når maks sats
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 20.01.2025 | 25.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 3000   |
      | 20.01.2025 | 25.01.2025 | 4000   |

    Når beregner boutgifter med revurderFra=2025-01-22

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 07.01.2025 | 25.01.2025 | 1              | 4953         | 4953      | 07.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |
