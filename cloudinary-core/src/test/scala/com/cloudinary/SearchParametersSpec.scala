package com.cloudinary

import com.cloudinary.Api.{ASCENDING, DESCENDING}
import com.cloudinary.parameters.SearchParameters
import org.json4s.native.Serialization.write
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inside, OptionValues}

class SearchParametersSpec extends AnyFlatSpec with Matchers with OptionValues with Inside {
  import SearchParameters.formats

  behavior of "SearchParameters"

  it should "create empty json if no field specified" in {
    write(SearchParameters()) should equal("{}")
  }

  it should "serialize expression" in {
    write(SearchParameters(expression = Some("format:jpg"))) should equal("{\"expression\":\"format:jpg\"}")
  }

  it should "serialize sort_by" in {
    write(SearchParameters(sortBy = List(("created_at" -> ASCENDING), ("updated_at" -> DESCENDING)))) should equal(
      "{\"sort_by\":[{\"created_at\":\"asc\"},{\"updated_at\":\"desc\"}]}")
  }

  it should "serialize max_results" in {
    write(SearchParameters(maxResults = Some(10))) should equal("{\"max_results\":10}")
  }

  it should "serialize next_cursor" in {
    write(SearchParameters(nextCursor = Some("abcdefg"))) should equal("{\"next_cursor\":\"abcdefg\"}")
  }

  it should "serialize aggregate" in {
    write(SearchParameters(aggregate = List("format", "size_category"))) should equal(
      "{\"aggregate\":[\"format\",\"size_category\"]}")
  }

  it should "serialize with_field" in {
    write(SearchParameters(withField = List("context", "tags"))) should equal(
      "{\"with_field\":[\"context\",\"tags\"]}")
  }
}
