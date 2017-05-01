package com.cloudinary

import com.cloudinary.response.SearchResponse
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

import scala.collection.mutable.ListBuffer

class Search() {
  private var _sortBy: Option[ListBuffer[(String, String)]] = None
  private var _aggregate: Option[ListBuffer[String]] = None
  private var _withField: Option[ListBuffer[String]] = None
  private var _maxResults: Option[Int] = None
  private var _nextCursor: Option[String] = None
  private var _expression: Option[String] = None

  def sortBy(name: String, direction: String): Search = {
    if (_sortBy.isEmpty) {
      _sortBy = Some(new ListBuffer[(String, String)])
    }
    _sortBy.get += (name -> direction)
    this
  }

  def expression(value: String): Search = {
    _expression = Some(value)
    this
  }

  def maxResults(value: Int): Search = {
    _maxResults = Some(value)
    this
  }

  def nextCursor(value: String): Search = {
    _nextCursor = Some(value)
    this
  }

  def aggregate(value: String): Search = {
    if (_aggregate.isEmpty) {
      _aggregate = Some(new ListBuffer[String])
    }
    _aggregate.get += value
    this
  }

  def withField(value: String): Search = {
    if (_withField.isEmpty) {
      _withField = Some(new ListBuffer[String])
    }
    _withField.get += value
    this
  }

  def toJSON(): String = {
    compact(render(
      ("expression" -> _expression) ~
        ("with_field" -> _withField) ~
        ("aggregate" -> _aggregate) ~
        ("sort_by" -> _sortBy) ~
        ("max_results" -> _maxResults) ~
        ("next_cursor" -> _nextCursor)
    ))
  }

  def execute() = new Cloudinary().api().callJsonApi[SearchResponse](Api.POST, "resources" :: "search" :: Nil, toJSON())
}
