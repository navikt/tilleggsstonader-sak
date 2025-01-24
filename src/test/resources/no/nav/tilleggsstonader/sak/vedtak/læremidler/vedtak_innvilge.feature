# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av læremiudler

  Scenario: Skal lagre ned samlet andeler for alle perioder med samme utbetalingsdato
    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.01.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.01.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.12.2024 | 31.01.2025 |

    Så forvent beregningsresultatet for behandling=1
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 02.12.2024      |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |

    Så forvent andeler for behandling=1
      | Fom        | Type           | Utbetalingsdato |
      | 02.12.2024 | LÆREMIDLER_AAP | 02.12.2024      |
      | 01.01.2025 | LÆREMIDLER_AAP | 01.01.2025      |