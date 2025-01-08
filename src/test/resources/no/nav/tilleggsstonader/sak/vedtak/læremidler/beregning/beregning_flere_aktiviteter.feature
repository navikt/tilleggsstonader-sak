# language: no
# encoding: UTF-8

Egenskap: Beregning læremidler - flere aktiviteter

  Scenario: Flere aktiviteter, kun en per utbetalingsperiode
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 30.04.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2024 | 29.02.2024 | TILTAK    | VIDEREGÅENDE     | 100           |
      | 01.03.2024 | 30.04.2025 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 30.04.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | AAP       | 01.01.2024         |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | AAP       | 01.01.2024         |
      | 01.03.2024 | 31.03.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | AAP       | 01.01.2024         |
      | 01.04.2024 | 30.04.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | AAP       | 01.01.2024         |

  Scenario: Flere aktiviteter i samme måned - overlappende hele måneden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.08.2024 | 30.09.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 15.08.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 15.08.2024 | 30.09.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Det finnes mer enn 1 aktivitet i perioden

  Scenario: Flere aktiviteter i samme måned - ingen dekker hele måneden som skal beregnes
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.08.2024 | 30.09.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 31.08.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 01.09.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 15.08.2024 | 30.09.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Det finnes ingen aktiviteter av type TILTAK som varer i hele perioden

  Scenario: Flere aktiviteter i samme måned - kun en gyldig type
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.08.2024 | 30.09.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 15.08.2024 | 30.09.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 15.08.2024 | 30.09.2024 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 15.08.2024 | 30.09.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 15.08.2024         |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 15.08.2024         |