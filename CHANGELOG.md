
2.0.0 / 2020-11-10
==================

  * Bump scala version to 2.12.8 (#29)
  * Drop support for scala 2.10.4

1.2.2 / 2019-07-26
==================

  * Use utf-8 encoding in API requests. 

1.2.1 / 2017-05-15
==================

  * Fix `resourcesByIds` to allow comma in public ids.
  * Refactor test to allow concurrent run.

1.2.0 / 2017-05-01
==================

New functionality and features
------------------------------

  * Add Search API

Other Changes
-------------

  * Merge pull request #19 from janghwan/master

1.1.2 / 2017-03-07
==================

  * Set read timeout to -1 to disable read timeout completely

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
