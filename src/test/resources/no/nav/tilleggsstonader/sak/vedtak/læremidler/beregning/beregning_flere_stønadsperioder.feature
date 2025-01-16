# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - flere stønadsperioder

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
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.01.2024      |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.01.2024      |
      | 01.03.2024 | 31.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.01.2024      |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.01.2024      |

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
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | AAP       | 01.04.2024      |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | AAP       | 01.04.2024      |
      | 01.08.2024 | 31.08.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | DAGPENGER | 01.08.2024      |
      | 01.09.2024 | 30.09.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | DAGPENGER | 01.08.2024      |

  Scenario: En vedtaksperiode som løper over flere stønadsperioder med ulike målgrupper
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2025 | 28.02.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 28.02.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 01.01.2025 | 31.01.2025 | AAP             | TILTAK    |
      | 01.02.2025 | 28.02.2025 | OVERGANGSSTØNAD | TILTAK    |

    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Vedtaksperiode er ikke innenfor en overlappsperiode

    #Så skal stønaden være
    #  | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe       | Utbetalingsdato |
    #  | 01.01.2025 | 31.01.2025 | 438   | VIDEREGÅENDE | 100           | 438  | AAP             | 01.01.2025      |
    #  | 01.02.2025 | 28.02.2025 | 438   | VIDEREGÅENDE | 100           | 438  | OVERGANGSSTØNAD | 03.02.2025      |


  Scenario: To uilke målgrupper samme aktivitet feiler i månedskiftet
    # TODO når man støtter flere målgrupper
    #  Scenario: To ulike målgrupper samme aktivitet
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

    Så forvent følgende feil fra læremidlerberegning: Vedtaksperiode er ikke innenfor en overlappsperiode
    # TODO når man støtter flere målgrupper
    # Så skal stønaden være
    #  | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
    #  | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.04.2024      |
    #  | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | DAGPENGER | 01.05.2024      |


  Scenario: To ulike målgrupper samme aktivitet feiler når ikke i månedskiftet
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

    Så forvent følgende feil fra læremidlerberegning: Vedtaksperiode er ikke innenfor en overlappsperiode

  Scenario: Stønadsperioder i ulike løpende måneder
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
    # TODO når man støtter flere målgrupper
    # Så skal stønaden være
    #  | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
    #  | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.04.2024      |
    #  | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.04.2024      |
