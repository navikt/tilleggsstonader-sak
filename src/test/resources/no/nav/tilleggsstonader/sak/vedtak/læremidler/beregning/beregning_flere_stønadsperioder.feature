# language: no
# encoding: UTF-8

Egenskap: Beregning

  Scenario: Flere stønadsperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 30.04.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.12.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 10.02.2024 | AAP       | TILTAK    |
      | 11.02.2024 | 05.03.2024 | AAP       | TILTAK    |
      | 06.03.2024 | 30.04.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsmåned |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.03.2024 | 31.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |

  Scenario: Flere stønadsperioder og vedtaksperioder med opphold
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.04.2024 | 31.05.2024 |
      | 01.08.2024 | 30.09.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | VIDEREGÅENDE     | 100           |
      | 01.08.2024 | 31.10.2024 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.04.2024 | 31.04.2024 | AAP       | TILTAK    |
      | 01.05.2024 | 31.05.2024 | AAP       | TILTAK    |
      | 01.08.2024 | 30.09.2024 | DAGPENGER | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe | Utbetalingsmåned |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | AAP       | 04.2024          |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | AAP       | 04.2024          |
      | 01.08.2024 | 31.08.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | DAGPENGER | 08.2024          |
      | 01.09.2024 | 30.09.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | DAGPENGER | 08.2024          |


  Scenario: To uilke målgrupper samme aktivitet funker i månedskiftet
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.04.2024 | 31.05.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.04.2024 | 31.04.2024 | AAP       | TILTAK    |
      | 01.05.2024 | 31.05.2024 | DAGPENGER | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsmåned |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 04.2024          |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | DAGPENGER | 04.2024          |


  Scenario: To uilke målgrupper samme aktivitet feiler når ikke i månedskiftet
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.04.2024 | 31.05.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.04.2024 | 04.05.2024 | AAP       | TILTAK    |
      | 05.05.2024 | 31.05.2024 | DAGPENGER | TILTAK    |


    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Det finnes ingen periode med overlapp mellom målgruppe og aktivitet for perioden

  Scenario: Flere stønadsperioder som unneholder utbeatlingsperioden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.04.2024 | 31.05.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | VIDEREGÅENDE | 100           |


    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.04.2024 | 31.04.2024 | AAP       | TILTAK    |
      | 01.05.2024 | 31.05.2024 | DAGPENGER | TILTAK    |
      | 01.04.2024 | 31.05.2024 | DAGPENGER | TILTAK    |


    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Det er for mange stønadsperioder som inneholder utbetalingsperioden
