# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av læremidler

  Scenario: Skal lagre ned samlet andeler for alle perioder med samme utbetalingsdato

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.12.2024 | 31.03.2025 |

    Så forvent beregningsresultatet for behandling=1
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 02.12.2024      |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.02.2025 | 31.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.03.2025 | 31.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 02.12.2024 | 438   | LÆREMIDLER_AAP | 02.12.2024      |
      | 01.01.2025 | 1353  | LÆREMIDLER_AAP | 01.01.2025      |

    Så forvent vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.12.2024 | 31.03.2025 |