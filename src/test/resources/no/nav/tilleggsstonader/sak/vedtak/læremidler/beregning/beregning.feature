# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler

  Scenario: VGS - 100%
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.03.2024 | 31.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |

  Scenario: VGS - 25%
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2024 | TILTAK    | VIDEREGÅENDE | 25            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 219   | VIDEREGÅENDE | 25            | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.02.2024 | 29.02.2024 | 219   | VIDEREGÅENDE | 25            | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.03.2024 | 31.03.2024 | 219   | VIDEREGÅENDE | 25            | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |

  Scenario: HØYERE_UTDANNING - 51%
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2024 | 31.03.2024 | TILTAK    | HØYERE_UTDANNING | 51            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 875   | HØYERE_UTDANNING | 51            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.02.2024 | 29.02.2024 | 875   | HØYERE_UTDANNING | 51            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.03.2024 | 31.03.2024 | 875   | HØYERE_UTDANNING | 51            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |

  Scenario: HØYERE_UTDANNING - 50%
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2024 | 31.03.2024 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.02.2024 | 29.02.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.03.2024 | 31.03.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |

  Scenario: Flere vedtaksperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.04.2024 | 31.05.2024 |
      | 15.08.2024 | 30.09.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2025 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.04.2024      |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.04.2024      |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |
