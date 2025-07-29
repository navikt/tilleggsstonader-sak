# language: no
# encoding: UTF-8

Egenskap: Utled tidligste endring med betydning for beregning

  Regel: Endring av sluttdato skal gi tidligste endring som tidligste av ny og gammel fom

    Scenario: Endring i aktivitet, ingen endring i vedtaksperioder
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | ENDRET |

      Gitt følgende vedtaksperioder i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 31.01.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 31.01.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.02.2024
      Så forvent ingen endring som påvirker utbetaling

    Scenario: Lagt til ny aktivitet etter vedtaksperiode, vedtaksperioder uendret
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status  |
        | 01.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | UENDRET |
        | 01.08.2024 | 31.08.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY      |

      Gitt følgende vedtaksperioder i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.02.2024 | 30.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.02.2024 | 30.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.08.2024
      Så forvent ingen endring som påvirker utbetaling


    Scenario: Ingen endring i aktivitet, vedtaksperioder forlenget
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status  |
        | 01.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | UENDRET |

      Gitt følgende vedtaksperioder i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 31.01.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 19.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.02.2024
      Så forvent følgende dato for tidligste endring som påvirker utbetaling: 01.02.2024

    Scenario: Lagt til ny aktivitet før vedtaksperiode, vedtaksperioder uendret
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status  |
        | 01.12.2023 | 31.12.2023 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY      |
        | 01.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | UENDRET |

      Gitt følgende vedtaksperioder i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.02.2024 | 20.02.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 20.02.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.12.2023
      Så forvent følgende dato for tidligste endring som påvirker utbetaling: 01.01.2024
