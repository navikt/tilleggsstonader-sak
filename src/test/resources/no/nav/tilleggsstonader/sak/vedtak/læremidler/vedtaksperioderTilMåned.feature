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

  Scenario: Flere vedtaksperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 17.04.2024 | 20.05.2024 |
      | 18.08.2024 | 04.10.2024 |
      | 13.12.2024 | 31.01.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 17.04.2024 | 16.05.2024 | 04.2024          |
      | 17.05.2024 | 20.05.2024 | 04.2024          |
      | 18.08.2024 | 17.09.2024 | 08.2024          |
      | 18.09.2024 | 04.10.2024 | 08.2024          |
      | 13.12.2024 | 31.12.2024 | 12.2024          |
      | 01.01.2025 | 31.01.2025 | 01.2025          |

  Scenario: Treffer rundt månedsskifte februar-mars - håndter spesialtilfelle
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.01.2024 | 31.08.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 31.01.2024 | 29.02.2024 | 01.2024          |
      | 01.03.2024 | 31.03.2024 | 01.2024          |
      | 01.04.2024 | 30.04.2024 | 01.2024          |
      | 01.05.2024 | 31.05.2024 | 01.2024          |
      | 01.06.2024 | 30.06.2024 | 01.2024          |
      | 01.07.2024 | 31.07.2024 | 01.2024          |
      | 01.08.2024 | 31.08.2024 | 01.2024          |

  Scenario: Treffer rundt månedsskifte februar-mars - håndter spesialtilfelle 2
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 29.01.2024 | 31.08.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 29.01.2024 | 29.02.2024 | 01.2024          |
      | 01.03.2024 | 31.03.2024 | 01.2024          |
      | 01.04.2024 | 30.04.2024 | 01.2024          |
      | 01.05.2024 | 31.05.2024 | 01.2024          |
      | 01.06.2024 | 30.06.2024 | 01.2024          |
      | 01.07.2024 | 31.07.2024 | 01.2024          |
      | 01.08.2024 | 31.08.2024 | 01.2024          |


  Scenario: Treffer rundt månedsskifte februar-mars - ingen spesialtilfelle
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 28.01.2024 | 27.04.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 28.01.2024 | 27.02.2024 | 01.2024          |
      | 28.02.2024 | 27.03.2024 | 01.2024          |
      | 28.03.2024 | 27.04.2024 | 01.2024          |


  Scenario: Fra lenger til kortere måned
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.03.2024 | 31.07.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsmåned |
      | 31.03.2024 | 30.04.2024 | 03.2024          |
      | 01.05.2024 | 31.05.2024 | 03.2024          |
      | 01.06.2024 | 30.06.2024 | 03.2024          |
      | 01.07.2024 | 31.07.2024 | 03.2024          |
