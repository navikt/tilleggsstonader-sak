# language: no
# encoding: UTF-8

Egenskap: Beregning læremidler - flere målgrupper - flere aktiviteter

  Scenario: Vedtaksperiode som overlapper med 2 ulike målgrupper og ulike aktiviteter
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 07.01.2025 | 08.01.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      # Overlapper ikke med stønadsperiode, med med vedtaksperiode
      | 01.01.2025 | 07.01.2025 | TILTAK    | HØYERE_UTDANNING | 100           |
      | 01.01.2025 | 07.01.2025 | UTDANNING | VIDEREGÅENDE     | 50            |
      | 08.01.2025 | 08.01.2025 | TILTAK    | VIDEREGÅENDE     | 50            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 07.01.2025 | 07.01.2025 | OVERGANGSSTØNAD | UTDANNING |
      | 08.01.2025 | 08.01.2025 | AAP             | TILTAK    |

    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Vedtaksperiode er ikke innenfor en overlappsperiode

    #Så skal stønaden være
    #  | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
    #  | 07.01.2025 | 08.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 06.01.2025      |
