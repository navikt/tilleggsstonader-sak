# language: no
# encoding: UTF-8

Egenskap: Beregning

  Scenario: Første enkleste case
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
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsmåned |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.03.2024 | 31.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |


  Scenario: Krysser nyttår
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.11.2024 | 28.02.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2025 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsmåned |
      | 15.11.2024 | 14.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 11.2024          |
      | 15.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 11.2024          |
      | 01.01.2025 | 31.01.2025 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2025          |
      | 01.02.2025 | 28.02.2025 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2025          |


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
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsmåned |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 04.2024          |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 04.2024          |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 08.2024          |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 08.2024          |


  Scenario: Første enkleste case - validering
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | UTDANNING |


    Når validerer vedtaksperiode for læremidler

    Så skal resultat fra validering være
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |