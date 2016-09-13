
0.9.10-SNAPSHOT / 2016-08-30
===================

New functionality and features
------------------------------

  * Add `next_cursor` to `transformation`
  * Add backward compatible signatures for `transformation` and `transformationByName`
  * Refactor `HttpClient` singleton.

Other Changes
-------------

  * Change `HttpClient.client` instead of creating a new instance of `HttpClient` when mocking.
  * Remove `BeforeAndAfter` from `ApiSpec`
  * Mock sprite test
  * Create `MockableFlatSpec`
  * Mock listing by start date test.
  * Add travis-ci setup
  * Add random suffix to IDs in tests.
  * Mock Api tests