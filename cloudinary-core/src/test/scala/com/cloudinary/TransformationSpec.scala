package com.cloudinary

import scala.language.postfixOps
import org.scalatest._
import org.scalatest.Matchers

class TransformationSpec extends FlatSpec with Matchers with OptionValues with Inside {
  behavior of "A Transformation"

  lazy val cloudinary = {
    new Cloudinary("cloudinary://a:b@test123")
  }
  
  it should "support background" in {
    cloudinary.url().transformation(Transformation().b_("red")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/b_red/test")
    cloudinary.url().transformation(Transformation().b_("#112233")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/b_rgb:112233/test")
  }
  
  it should "support default image" in {
    cloudinary.url().transformation(Transformation().d_("default")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/d_default/test")
  }
  
  it should "support angle" in {
    cloudinary.url().transformation(Transformation().a_(12)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/a_12/test")
    cloudinary.url().transformation(Transformation().a_("exif", "12")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/a_exif.12/test")
  }

  it should "support opacity" in {
    cloudinary.url().transformation(Transformation().o_(23)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/o_23/test")
    cloudinary.url().transformation(Transformation().opacity(77)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/o_77/test")
  }
  
  it should "support overlay" in {
    cloudinary.url().transformation(Transformation().l_("text:hello")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/l_text:hello/test")
  }
  
  it should "not pass width/height to html if overlay" in {
    val t = Transformation().l_("text:hello").w_(100).h_(100)
    t.htmlWidth should equal(None)
    t.htmlHeight should equal(None)
    val tControl = t / (Transformation().w_(110).h_(100))
    tControl.htmlWidth should equal(Some(110))
    tControl.htmlHeight should equal(Some(100))
    cloudinary.url().transformation(t).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/h_100,l_text:hello,w_100/test")
  }
  
  it should "support underlay" in {
    cloudinary.url().transformation(Transformation().u_("text:hello")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/u_text:hello/test")
  }
  
  it should "not pass width/height to html if underlay" in {
    val t = Transformation().u_("text:hello").w_(100).h_(100)
    t.htmlWidth should equal(None)
    t.htmlHeight should equal(None)
    val tControl = t / (Transformation().w_(110).h_(100))
    tControl.htmlWidth should equal(Some(110))
    tControl.htmlHeight should equal(Some(100))
    cloudinary.url().transformation(t).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/h_100,u_text:hello,w_100/test")
  }
  
  it should "support format for fetch urls" in {
    cloudinary.url().format("jpg").`type`("fetch").generate("http://cloudinary.com/images/logo.png") should equal(
      "http://res.cloudinary.com/test123/image/fetch/f_jpg/http://cloudinary.com/images/logo.png")
  }
  
  it should "support effect" in {
    cloudinary.url().transformation(Transformation().e_("sepia")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/e_sepia/test")
  }
  
  it should "support flags" in {
    cloudinary.url().transformation(Transformation().fl_("abc")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/fl_abc/test")
  }
  
  it should "support multiple flags" in {
    cloudinary.url().transformation(Transformation().fl_("abc","efg")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/fl_abc.efg/test")
  }
  
  it should "support effect with param" in {
    cloudinary.url().transformation(Transformation().e_("sepia", 10)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/e_sepia:10/test")
  }
  
  it should "support x and y" in {
    cloudinary.url().transformation(Transformation().x_(10)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/x_10/test")
    cloudinary.url().transformation(Transformation().y_(10)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/y_10/test")
    cloudinary.url().transformation(Transformation().x_(10).y_(20)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/x_10,y_20/test")
  }
  
  it should "support cropping" in {
    val t = Transformation().w_(100).h_(101)
    cloudinary.url().transformation(t).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/h_101,w_100/test")
    t.htmlHeight should equal(Some(101))
    t.htmlWidth should equal(Some(100))
    cloudinary.url().transformation(t.crop("crop")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/c_crop,h_101,w_100/test")
  }
  
  it should "support various options" in {
    cloudinary.url().transformation(Transformation().x(1).y(2).r_(3).g_("center").q_(0.4).p_("a")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/g_center,p_a,q_0.4,r_3,x_1,y_2/test")
  }
  
  it should "support named transformation" in {
    cloudinary.url().transformation(Transformation().named("name")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/t_name/test")
  }
  
  it should "support multiple named transformation" in {
    cloudinary.url().transformation(Transformation().named("name1","name2")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/t_name1.name2/test")
  }
  
  it should "support base transformation" in {
    cloudinary.url().transformation(Transformation().x(100).y(100).c_("fill")./.c_("crop").w_(101)).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/c_fill,x_100,y_100/c_crop,w_101/test")
  }
  
  it should "support array of base transformations" in {
    val t = Transformation().x(100).y(100).w_(200).c_("fill")./.r_(10)./.c_("crop").w_(100)
    t.htmlWidth should equal(Some(100))
	cloudinary.url().transformation(t).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/c_fill,w_200,x_100,y_100/r_10/c_crop,w_100/test")
  }
  
  it should "not include empty transformations" in {
    val t = Transformation()./.x(100).y(100).c_("fill")./
	cloudinary.url().transformation(t).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/c_fill,x_100,y_100/test")
  }
  
  it should "support circled" in {
    cloudinary.url().transformation(Transformation().circled()).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/r_max/test")
  }
  
  it should "support density" in {
    val transformation = Transformation().dn_(150)
    cloudinary.url().transformation(transformation).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/dn_150/test")
  }
  
  it should "support page" in {
    val transformation = Transformation().pg_(5)
    cloudinary.url().transformation(transformation).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/pg_5/test")
  }
  
  it should "support border" in {
    cloudinary.url()
    .transformation(Transformation().bo_(5, "black")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/bo_5px_solid_black/test")
    
    cloudinary.url()
    .transformation(Transformation().bo_(5, "#ffaabbdd")).generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/bo_5px_solid_rgb:ffaabbdd/test")
  }

}