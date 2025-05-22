# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av boutgifter - revurdering

  Bakgrunn:
    Gitt følgende oppfylte aktiviteter for behandling=1
      | Fom        | Tom        | Aktivitet |
      | 01.01.2024 | 31.03.2026 | TILTAK    |

    Og følgende oppfylte målgrupper for behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2024 | 31.03.2026 | AAP       |

  Scenario: Legger til ny samling(feb) etter forrige samling(jan) som havner i ny beregningsperiode
  Resultat: Blir en ny utbetalingsperiode, som utbetales i februar
    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 07.01.2025 | 09.01.2025 |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato |
      | 07.01.2025 | 06.02.2025 | 1000         | 4953      | 07.01.2025      |

    Når vi kopierer perioder fra forrige behandling for behandling=2

    Og vi legger inn følgende nye utgifter av type UTGIFTER_OVERNATTING for behandling=2
      | Fom        | Tom        | Utgift |
      | 15.02.2025 | 18.02.2025 | 3000   |

    Og vi innvilger boutgifter behandling=2 med revurderFra=2025-01-09 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 07.01.2025 | 09.01.2025 |
      | 15.02.2025 | 18.02.2025 |

    Så kan vi forvente følgende beregningsresultat for behandling=2
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Del av tidligere utbetaling |
      | 07.01.2025 | 06.02.2025 | 1000         | 4953      | 07.01.2025      | Ja                          |
      | 15.02.2025 | 14.03.2025 | 3000         | 4953      | 15.02.2025      | Nei                         |

    Og følgende vedtaksperioder for behandling=2
      | Fom        | Tom        |
      | 07.01.2025 | 09.01.2025 |
      | 15.02.2025 | 18.02.2025 |

    Og følgende andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 07.01.2025      |
      | 03.02.2025 | 3000  | BOUTGIFTER_AAP | 17.02.2025      |

  Scenario: Legger til ny samling en uke etter forrige samling
  Resultat: Den eksisterende beregningsperioden blir oppdatert, med samme utbetalingsdato som før
    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 25.02.2025 | 25.02.2025 | 3000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 25.02.2025 | 25.02.2025 |

    Og vi kopierer perioder fra forrige behandling for behandling=2

    Og vi legger inn følgende nye utgifter av type UTGIFTER_OVERNATTING for behandling=2
      | Fom        | Tom        | Utgift |
      | 04.03.2025 | 06.03.2025 | 4000   |

    Og vi innvilger boutgifter behandling=2 med revurderFra=2025-01-09 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 25.02.2025 | 25.02.2025 |
      | 04.03.2025 | 06.03.2025 |

    Så kan vi forvente følgende beregningsresultat for behandling=2
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Del av tidligere utbetaling |
      | 25.02.2025 | 24.03.2025 | 4953         | 4953      | 25.02.2025      | Ja                          |

    Og følgende vedtaksperioder for behandling=2
      | Fom        | Tom        |
      | 25.02.2025 | 25.02.2025 |
      | 04.03.2025 | 06.03.2025 |

    Og følgende andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 03.02.2025 | 4953  | BOUTGIFTER_AAP | 25.02.2025      |

    Men følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 03.02.2025 | 3000  | BOUTGIFTER_AAP | 25.02.2025      |

  Scenario: Aktiviteten blir forkortet, men utgiftene per måned øker
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.08.2024 | 31.12.2024 | 3000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 17.08.2024 | 20.12.2024 |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato |
      | 17.08.2024 | 16.09.2024 | 3000         | 4809      | 17.08.2024      |
      | 17.09.2024 | 16.10.2024 | 3000         | 4809      | 17.09.2024      |
      | 17.10.2024 | 16.11.2024 | 3000         | 4809      | 17.10.2024      |
      | 17.11.2024 | 16.12.2024 | 3000         | 4809      | 17.11.2024      |
      | 17.12.2024 | 20.12.2024 | 3000         | 4809      | 17.12.2024      |

    Når vi kopierer perioder fra forrige behandling for behandling=2

    Men vi fjerner utgiftene på behandling=2

    Og vi legger inn følgende nye utgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=2
      | Fom        | Tom        | Utgift |
      | 01.08.2024 | 31.10.2024 | 4500   |

    Og vi innvilger boutgifter behandling=2 med revurderFra=2024-08-17 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 17.08.2024 | 20.10.2024 |

    Så kan vi forvente følgende beregningsresultat for behandling=2
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Del av tidligere utbetaling |
      | 17.08.2024 | 16.09.2024 | 4500         | 4809      | 17.08.2024      | Ja                          |
      | 17.09.2024 | 16.10.2024 | 4500         | 4809      | 17.09.2024      | Ja                          |
      | 17.10.2024 | 20.10.2024 | 4500         | 4809      | 17.10.2024      | Ja                          |

    Og følgende andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.08.2024 | 4500  | BOUTGIFTER_AAP | 19.08.2024      |
      | 02.09.2024 | 4500  | BOUTGIFTER_AAP | 17.09.2024      |
      | 01.10.2024 | 4500  | BOUTGIFTER_AAP | 17.10.2024      |

  Scenario: Aktiviteten blir forskjøvet en uke framover i tid, inn i neste måned
  Resultat: Forventer nye nye beregningsperioder, mens andelen får ny dato
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 30.02.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 25.01.2025 | 26.01.2025 |

    Så kan vi forvente følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 27.01.2025      |

    Når vi kopierer perioder fra forrige behandling for behandling=2

    Og vi innvilger boutgifter behandling=2 med revurderFra=2025-01-25 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 03.02.2025 | 04.02.2025 |

    Så kan vi forvente følgende andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 03.02.2025 | 1000  | BOUTGIFTER_AAP | 03.02.2025      |

  Scenario: Har faste utgifter fra før, legger inn en midlertidig overnatting
  Resultat: Forventer feilmelding, ettersom det ikke er støttet i løsningen enda
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Og vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.01.2025 | 31.01.2025 |

    Og vi kopierer perioder fra forrige behandling for behandling=2

    Og vi legger inn følgende nye utgifter av type UTGIFTER_OVERNATTING for behandling=2
      | Fom        | Tom        | Utgift |
      | 15.02.2025 | 16.02.2025 | 1000   |

    Når vi innvilger boutgifter behandling=2 med revurderFra=15.02.2025 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.01.2025 | 31.01.2025 |
      | 15.02.2025 | 16.02.2025 |

    Så forvent følgende feilmelding: Foreløpig støtter vi ikke løpende og midlertidige utgifter i samme behandling
