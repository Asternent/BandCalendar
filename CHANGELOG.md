# Changes

## 0.2.0
- Preview-first date selection; full details on repeated selection or summary tap.
- Year/month quick jump with cancellation, anchored day-of-month navigation, explicit bounds.
- Previous/today/next-day detail navigation and consistent back stack.
- Dedicated scope/sync menu, session scope override and follow-phone restoration.
- Non-disruptive deferred preference application and single-flight sync with retry.
- Material dark hierarchy, month-aware adjacent cells, larger touch targets, holiday cards.

## 0.1.4
- Reuse all 42 month slots across month/filter transitions; remove 42 native layout wrappers.
- Lightweight summary with zero worldwide detail decoding until a detail page is opened.
- O(1) bounded ring caches and 42-date/one-UTC cold-month generation.
- Defer hidden month updates and interconnect startup; reuse detail rows and guard after-layout scroll callbacks.
- Regression coverage for rapid navigation, stale callbacks, background sync and midnight markers.

## 0.1.3
- Date numbers and day-cell fills distinguish Mainland China official rest, make-up work, and ordinary weekends.
- Today/selection never override the rest/work date colour.
- Month overview and selected-date details show full official holiday ranges and consecutive day counts.
- Cross-month ranges retain full lengths; unknown years and foreign festivals never imply Mainland China rest days.
- Added 1,461-day and 48-month rest-kind/range parity checks.

## 0.1.2
- Both calendars show coloured festival abbreviations; tap a day for all events.
- Mainland China rest and compensatory workday badges remain separate.
- Lightweight month marker index; no full worldwide event decoding for 42 cells.
- Binary-search lunar lookup and bounded month/day/detail caches.
- Stable month component tree, two-cell selection patching, lazy retained details.
- Detail pages show eight groups at a time without dropping any events.
- Added calendar-marker parity and structural performance regression tests.

## 0.1.1
- Corrected scroll-transition race and bundled source/attribution access.
- Added known Mainland China festival dates without inferring future rest days.

## 0.1.0
- Initial offline global calendar and Android preference synchronizer trial.
