package io.citrine.lolo.trees.classification

import io.citrine.lolo.{Learner, PredictionResult}
import io.citrine.lolo.trees.{InternalModelNode, ModelNode, TrainingLeaf, TrainingNode}
import io.citrine.lolo.trees.splits.{ClassificationSplitter, NoSplit, Split}

/**
  * Created by maxhutch on 1/12/17.
  */
class ClassificationTrainingNode(
                                  trainingData: Seq[(Vector[AnyVal], Char, Double)],
                                  leafLearner: Learner,
                                  split: Split,
                                  deltaImpurity: Double,
                                  numFeatures: Int,
                                  minLeafInstances: Int,
                                  remainingDepth: Int,
                                  maxDepth: Int
                                ) extends TrainingNode(trainingData, remainingDepth) {

  assert(trainingData.size > 1, "If we are going to split, we need at least 2 training rows")
  assert(!split.isInstanceOf[NoSplit], s"Empty split split for training data: \n${trainingData.map(_.toString() + "\n")}")

  lazy val (leftTrain, rightTrain) = trainingData.partition(r => split.turnLeft(r._1))
  assert(leftTrain.size > 0 && rightTrain.size > 0, s"Split ${split} resulted in zero size: ${trainingData.map(_._1(split.getIndex()))}")

  lazy val leftChild = ClassificationTrainingNode.buildChild(leftTrain, leafLearner, minLeafInstances, remainingDepth, maxDepth, numFeatures)

  lazy val rightChild = ClassificationTrainingNode.buildChild(rightTrain, leafLearner, minLeafInstances, remainingDepth, maxDepth, numFeatures)


  /**
    * Get the lightweight prediction node for the output tree
    *
    * @return lightweight prediction node
    */
  override def getNode(): ModelNode[PredictionResult[Char]] = new InternalModelNode(
    split, leftChild.getNode(), rightChild.getNode()
  )

  override def getFeatureImportance(): scala.collection.mutable.ArraySeq[Double] = {
    val improvement = deltaImpurity
    var ans = leftChild.getFeatureImportance().zip(rightChild.getFeatureImportance()).map(p => p._1 + p._2)
    ans(split.getIndex) = ans(split.getIndex) + improvement
    ans
  }
}

object ClassificationTrainingNode {
  /**
    * Build a child node by pre-computing a split
    *
    * If there isn't a split, the child is a leaf; otherwise, the child is
    * another training node
    *
    * @param trainingData     for the child
    * @param leafLearner      to pass through
    * @param minLeafInstances minimum training instances per node
    * @param remainingDepth   the number of splits left
    * @param maxDepth         to compute depth via remainingDepth
    * @param numFeatures      to consider in the split
    * @return the child node, either a RegressionTrainingNode or TrainingLeaf
    */
  def buildChild(
                  trainingData: Seq[(Vector[AnyVal], Char, Double)],
                  leafLearner: Learner,
                  minLeafInstances: Int,
                  remainingDepth: Int,
                  maxDepth: Int,
                  numFeatures: Int
                ): TrainingNode[AnyVal, Char] = {
    if (trainingData.size >= 2 * minLeafInstances && remainingDepth > 0 && trainingData.exists(_._2 != trainingData.head._2)) {
      val (leftSplit, leftDelta) = ClassificationSplitter.getBestSplit(trainingData, numFeatures, minLeafInstances)
      if (!leftSplit.isInstanceOf[NoSplit]) {
        new ClassificationTrainingNode(trainingData, leafLearner, leftSplit, leftDelta, numFeatures, minLeafInstances, remainingDepth - 1, maxDepth)
      } else {
        new TrainingLeaf(trainingData, leafLearner, maxDepth - remainingDepth)
      }
    } else {
      new TrainingLeaf(trainingData, leafLearner, maxDepth - remainingDepth)
    }
  }
}
