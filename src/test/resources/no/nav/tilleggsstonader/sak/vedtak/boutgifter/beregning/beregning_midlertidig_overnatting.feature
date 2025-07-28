# language: no
# encoding: UTF-8

Egenskap: Beregning av midlertidig overnatting

  Bakgrunn:
    Gitt følgende oppfylte aktiviteter for behandling=1
      | Fom        | Tom        | Aktivitet |
      | 01.01.2024 | 01.01.2026 | TILTAK    |

    Og følgende oppfylte målgrupper for behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2024 | 01.01.2026 | AAP       |

  Scenario: Innvilgelse av midlertidlig overnatting

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 07.01.2025 | 06.02.2025 | 1000         | 4953      | 07.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 07.01.2025      |

  Scenario: Vedtaksperiode krysser nyttår

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 30.12.2024 | 02.01.2025 | 8000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 30.12.2024 | 02.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 30.12.2024 | 29.01.2025 | 4809         | 4809      | 30.12.2024      | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 02.12.2024 | 4809  | BOUTGIFTER_AAP | 30.12.2024      |

  Scenario: Flere vedtaksperioder med ulik utbetalingsperiode

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |
      | 12.03.2025 | 15.03.2025 | 8000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 12.03.2025 | 15.03.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 07.01.2025 | 06.02.2025 | 1000         | 4953      | 07.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 12.03.2025 | 11.04.2025 | 4953         | 4953      | 12.03.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 07.01.2025      |
      | 03.03.2025 | 4953  | BOUTGIFTER_AAP | 12.03.2025      |

  Scenario: Flere vedtaksperioder med samme utbetalingsperiode

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |
      | 21.01.2025 | 24.01.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 21.01.2025 | 24.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 07.01.2025 | 06.02.2025 | 2000         | 4953      | 07.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 2000  | BOUTGIFTER_AAP | 07.01.2025      |

  Scenario: Vedtaksperiode kun i helg

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 11.01.2025 | 12.01.2025 | 1500   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 11.01.2025 | 12.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 11.01.2025 | 10.02.2025 | 1500         | 4953      | 11.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1500  | BOUTGIFTER_AAP | 13.01.2025      |

  Scenario: Vedtaksperioder krysser utbetalingsperioder skal gi feilmelding ettersom det ikke støttes i løsningen enda
    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 11.01.2025 | 12.01.2025 | 1500   |
      | 09.02.2025 | 14.02.2025 | 1500   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 11.01.2025 | 12.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 09.02.2025 | 14.02.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så forvent følgende feilmelding:
     """
    Systemet klarer ikke å beregne utgifter til overnatting som krysser beregningsperioder
    Følgende perioder med overnatting må splittes i to perioder for å kunne beregne

    09.02.2025–14.02.2025 må splittes til 11.01.2025–10.02.2025 og 11.02.2025–10.03.2025
    """

  Scenario: To påfølgende vedtaksperioder som dekker utgiften
    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 15.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 16.01.2025 | 31.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Utgift som strekker seg utenfor en vedtaksperiode, er ikke støttet enda og skal skal gi feilmelding

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 15.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så forvent følgende feilmelding: Vi har foreløpig ikke støtte for å beregne når utgifter til midlertidig overnatting strekker seg utenfor vedtaksperiodene.

  Scenario: Utgifter som er helt utenfor vedtaksperiodene er OK, selv om de krysser beregningsperioder

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 10.01.2025 | 1000   |
      | 01.02.2025 | 10.02.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.02.2025 | 10.02.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.02.2025 | 28.02.2025 | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Skal få dekket for faktiske utgifter

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift | Høyere utgifter |
      | 01.01.2025 | 10.01.2025 | 20000  | Ja              |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 10.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet | Høyere utgifter |
      | 01.01.2025 | 31.01.2025 | 20000        | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    | Ja              |

  Scenario: Kombinasjon av høyere utgifter og ikke høyere utgifter i samme utbetalingsperiode er ikke støttet

    Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
      | Fom        | Tom        | Utgift | Høyere utgifter |
      | 01.01.2025 | 10.01.2025 | 20000  | Ja              |
      | 11.01.2025 | 11.01.2025 | 20000  | Nei             |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 11.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Så forvent følgende feilmelding: Vi støtter ikke at en person både skal få dekket faktiske utgifter og ikke faktiske utgifter i samme utbetalingsperiode
