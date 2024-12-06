# language: no
# encoding: UTF-8

Egenskap: Beregning

  Scenario: Første enkleste case
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2024 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | UTDANNING |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  |
      | 01.03.2024 | 31.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  |

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