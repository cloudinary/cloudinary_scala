package com.cloudinary

import com.cloudinary.Api.{ASCENDING, ListDirection}
import com.cloudinary.parameters.SearchParameters
import com.cloudinary.response.SearchResponse

case class Search(parameters: SearchParameters = SearchParameters())(implicit cloudinary: Cloudinary) {

  def expression(expression: String) = Search(parameters.copy(expression = Some(expression)))

  def maxResults(maxResults: Int) = Search(parameters.copy(maxResults = Some(maxResults)))

  def nextCursor(nextCursor: String) = Search(parameters.copy(nextCursor = Some(nextCursor)))

  def aggregate(field: String) = Search(parameters.copy(aggregate = parameters.aggregate ::: List(field)))

  def withField(field: String) = Search(parameters.copy(withField = parameters.withField ::: List(field)))

  def sortBy(field: String, direction: ListDirection = ASCENDING) = Search(parameters.copy(sortBy = parameters.sortBy ::: List((field -> direction))))

  def execute = {
    import SearchParameters.formats
    new Api().callJsonApi[SearchParameters, SearchResponse](Api.POST, List("resources", "search"), parameters)
  }
}
