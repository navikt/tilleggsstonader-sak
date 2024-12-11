# language: no
# encoding: UTF-8

Egenskap: Splitt vedtaksperioder til utbetalingsperioder

  Scenario: En vedtaksperiode innenfor et år, start i starten av måneden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.08.2024 | 30.09.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 01.08.2024 | 31.08.2024 | 08.2024          |
      | 01.09.2024 | 30.09.2024 | 08.2024          |

  Scenario: En vedtaksperiode innenfor et år, start i midten av måneden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.08.2024 | 30.09.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 15.08.2024 | 14.09.2024 | 08.2024          |
      | 15.09.2024 | 30.09.2024 | 08.2024          |

  Scenario: En vedtaksperiode som treffer nytt år
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.11.2024 | 14.01.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 15.11.2024 | 14.12.2024 | 11.2024          |
      | 15.12.2024 | 31.12.2024 | 11.2024          |
      | 01.01.2025 | 14.01.2025 | 01.2025          |

    # TODO: Rundt februar
# TODO: En test med flere vedtaksperioder (stopp på sommer)