1.1.1 / 2017-02-23
==================
  * Fix version number

1.1.0 / 2017-02-23
==================

  * Add `requestTimeout` argument in upload
  * Fix face coordinates test

1.0.0 / 2016-09-20
============================
  * First non-snapshot release

0.9.11-SNAPSHOT / 2016-09-13
============================

  * Support UTF-8 values
    * Fix creation of StringPart - use contentType and Charset. Add tests for Unicode public IDs

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
