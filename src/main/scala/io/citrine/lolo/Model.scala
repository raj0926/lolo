package io.citrine.lolo

/**
  * Created by maxhutch on 11/14/16.
  */
abstract class Model {

  def transform(inputs: Seq[Vector[Any]]): Seq[Vector[Any]]

}
