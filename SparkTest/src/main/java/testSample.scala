/**
  * Created by Plawan on 4/25/16.
  */
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object testSample {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    sc.setLogLevel("WARN")
//    val hdfsInputFile = "hdfs://localhost:9000/user/plawan/Papers-sub.txt"
    val hdfsInputFile = args(0)
//    val hdfsOutputPath = "hdfs://localhost:9000/user/plawan"
    val hdfsOutputPath = args(1)
    val testFile = sc.textFile(hdfsInputFile)
    val id = "06097A52"
    val result = testFile.flatMap(line => line.split("\\s+")).filter(x => x.equals(id));
    result.saveAsTextFile(hdfsOutputPath + "/sampleResJar")
  }
}