# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - periode som begynner i sluttet på en måned

  Scenario: Periode som begynner siste januar skal påbegynne neste periode siste februar
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.01.2024 | 31.05.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.05.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 31.01.2024 | 28.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 31.01.2024      |
      | 29.02.2024 | 28.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 31.01.2024      |
      | 29.03.2024 | 28.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 31.01.2024      |
      | 29.04.2024 | 28.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 31.01.2024      |
      | 29.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 31.01.2024      |

  Scenario: Periode som begynner siste april skal påbegynne neste periode fra og med nest siste mai, som er en måned frem i tiden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 30.04.2024 | 31.05.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | HØYERE_UTDANNING | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.05.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 30.04.2024 | 29.05.2024 | 875   | HØYERE_UTDANNING | 100           | 875  | AAP       | 30.04.2024      |
      | 30.05.2024 | 31.05.2024 | 875   | HØYERE_UTDANNING | 100           | 875  | AAP       | 30.04.2024      |