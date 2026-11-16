# AI Usage Disclosure – Milestone 5 (Performance, Security, Release)

## Nguyễn Nam Khoa – Performance (`feature/m5-performance`)

- **Tool used:** Claude (Claude Code)
- **What it did:**
  - Added LeakCanary 2.14 as `debugImplementation` (debug builds only).
  - Rebuilt `fragment_currency_converter.xml`: `rvRates` was `wrap_content` inside a `NestedScrollView`
    and created all 166 rows; it is now a `match_parent` list under an `AppBarLayout`, so rows are recycled.
    `CurrencyRepository` shows and stores only the six currencies the app offers.
  - `setHasFixedSize(true)` on the four full-height lists.
  - `CurrencyRepositoryTest` (2 unit tests) and the audit of every RecyclerView and every `Dispatchers.IO`
    use in [milestone5/performance_audit.md](milestone5/performance_audit.md).
- **How I checked it:** see the checklist in the pull request (unit tests, LeakCanary, Memory Profiler,
  release test of Exchange rates / Dashboard / Report).
