# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - flere aktiviteter v2

  Scenario: Flere aktiviteter, kun en per utbetalingsperiode
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 01.01.2024 | 30.04.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2024 | 29.02.2024 | TILTAK    | VIDEREGÅENDE     | 100           |
      | 01.03.2024 | 30.04.2025 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2024 | 30.04.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.03.2024 | 31.03.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.04.2024 | 30.04.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |

  Scenario: Flere aktiviteter, kun en per vedtaksperiode innenfor en måned
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 01.01.2024 | 12.01.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 05.01.2024 | TILTAK    | VIDEREGÅENDE | 30            |
      | 08.01.2024 | 12.01.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2024 | 05.01.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 08.01.2024 | 12.01.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 12.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |

  Scenario: Flere aktiviteter i samme måned - overlappende hele måneden
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 15.08.2024 | 30.09.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 30            |
      | 15.08.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 40            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 30.09.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 70            | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 70            | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere aktiviteter som overlapper av samme type studienivå, der en aktivitet løper lengre enn den andre
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 15.08.2024 | 30.09.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 14.09.2024 | TILTAK    | VIDEREGÅENDE | 30            |
      | 15.08.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 40            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 30.09.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 70            | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |
      | 15.09.2024 | 30.09.2024 | 219   | VIDEREGÅENDE | 40            | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere aktiviteter i samme måned - ingen dekker hele måneden som skal beregnes
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        |
      | 15.08.2024 | 30.09.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 31.08.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 01.09.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 30.09.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere aktiviteter i samme måned - kun en gyldig type
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 15.08.2024 | 30.09.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 15.08.2024 | 30.09.2024 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 30.09.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere aktiviteter med ulike studieprosent og studienivå
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 15.08.2024 | 30.09.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 15.08.2024 | 14.09.2024 | TILTAK    | VIDEREGÅENDE     | 100           |
      | 20.08.2024 | 30.09.2024 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 30.09.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så forvent følgende feil fra læremidlerberegning: Det er foreløpig ikke støtte for flere aktiviteter

  Scenario: Flere aktiviteter med ulike datoer innenfor en vedtaksperiode
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 15.08.2024 | 14.09.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 16.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 17.08.2024 | 18.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 19.08.2024 | 14.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 14.09.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere aktiviteter med ulike datoer innenfor løpende måned men utenfor vedtaksperiode
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 15.08.2024 | 15.08.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 15.08.2024 | 15.08.2024 | TILTAK    | VIDEREGÅENDE     | 50            |
      # aktivitet 2 er innenfor løpende måned, men ikke overlapp med vedtaksperiode
      | 20.08.2024 | 25.08.2024 | TILTAK    | HØYERE_UTDANNING | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 15.08.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 15.08.2024 | 15.08.2024 | 219   | VIDEREGÅENDE | 50            | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere aktiviteter med ulike datoer innenfor en og samme vedtaksperiode
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.01.2025 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2025 | 09.01.2025 | TILTAK    | VIDEREGÅENDE     | 50            |
      | 10.01.2025 | 31.01.2025 | TILTAK    | HØYERE_UTDANNING | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 901   | HØYERE_UTDANNING | 100           | 901  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
