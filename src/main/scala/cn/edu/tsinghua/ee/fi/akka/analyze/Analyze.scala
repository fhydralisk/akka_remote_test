package cn.edu.tsinghua.ee.fi.akka.analyze


import scala.io.Source


/**
  * Created by hydra on 2017/3/28.
  */
object Analyze {

  val aggs = Array(
    "理想情况15" -> (
      List(
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog0-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog5-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog10-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog15-1500-ns.log"

      ),
      List("0%", "5%", "10%", "15%")
    ),
    "理想情况30" -> (
      List(
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog0-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog5-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog10-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog15-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog20-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog25-1500-ns.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog30-1500-ns.log"

      ),
      List("0%", "5%", "10%", "15%", "20%", "25%", "30%")
    ),
    "iperf情况" -> (
      List(
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog0-1500-ns-iperf.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog5-1500-ns-iperf.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog10-1500-ns-iperf.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog15-1500-ns-iperf-2.log"
      ),
      List("0%", "5%", "10%", "15%")
    ),
    "UserService" -> (
      List(
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog0-1500-ns-iperf.log",
        "/Users/hydra/Documents/AtTsinghua/odl-all/akka-test-results/testlog10-1500-s.log"
      ),
      List("0%","10%")
    )
  )
  val select = 3
  val outputFilename = aggs(select)._1
  val agg = aggs(select)._2._1
  val legendHelper = aggs(select)._2._2

  val windowSize = 180

  def main(args: Array[String]): Unit = {
    import breeze.plot._

    // val filename = args(0)
    val files = agg map Source.fromFile
    val titles = files map { _.getLines().toIterable.head }
    val x = files map {
      _.getLines().toIterable.tail map { s =>
        s.trim().split('|') map {
          _.toDouble / 1000000
        }
      }
    }

    val latency = x map { _ map { _(0) }  toArray }
    val interval = x map { _ map { _(1) }  filter { _ != -1} toArray }

    val pktloss = latency map { _ count( _ == -1) }
    val latency_filtered = latency map { _ filter { _ != -1 } }

//    println(s"${latency_filtered(1).length}, ${interval(1).length}")

    val meanLatency = latency_filtered map { meanArray(_) }
    val stdLatency = latency_filtered map { stderrArray(_) }

    val meanInterval = interval map { meanArray(_) }
    val stdInterval = interval map { stderrArray(_) }

    val windowLatency = latency_filtered map { e => windowArray(e, windowSize) }
    val windowInterval = interval map { e => windowArray(e, windowSize) }

    /*val breezeLatency = DenseVector(latency_filtered)
    val breezeInterval = DenseVector(interval)*/

    //println(s"pktloss: ${pktloss.toFloat/latency.size.toFloat}")
    val pRow = 4
    val pCol = 2
    val f = Figure()
    val f2 = Figure()

    f.width = 1600
    f.height = 900

    f2.width = 1600
    f2.height = 900

    var i = 0
    latency_filtered zip titles foreach { case (lf, title) =>
      val pLatency = f2.subplot(latency_filtered.length, 2, i*2)
      pLatency.title = s"Latency PDF ($title)"
      pLatency += hist(lf, 50)
      i += 1
    }

    i = 0
    interval zip titles foreach { case (il, title) =>
      val pInterval = f2.subplot(interval.length, 2, i*2+1)
      pInterval.title = s"Interval PDF ($title)"
      pInterval += hist(il, 50)
      i += 1
    }

    val pWindowMeanLatency = f.subplot(pRow, pCol, 0)
    pWindowMeanLatency.title = s"Windowed($windowSize) Mean of Latency"
    windowLatency zip legendHelper foreach { case (wl, legend) =>
      pWindowMeanLatency += plot((1 to wl.length).map { _.toDouble }, wl.map { _._1 }, name = legend)
      //pWindowMeanLatency += plot((1 to wl.length).map { _.toDouble }, wl.map { _ => meanLatency })
    }
    pWindowMeanLatency.legend = true


    val pWindowMeanInterval = f.subplot(pRow, pCol, 1)
    pWindowMeanInterval.title = s"Windowed($windowSize) Mean of Interval"
    windowInterval foreach { wi =>
      pWindowMeanInterval += plot((1 to wi.length).map { _.toDouble }, wi.map { _._1 })
      // pWindowMeanInterval += plot((1 to windowInterval.length).map { _.toDouble }, windowInterval.map { _ => meanInterval })
    }

    val pWindowStdLatency = f.subplot(pRow, pCol, 2)
    pWindowStdLatency.title = "Windowed Std of Latency"
    windowLatency foreach { wl =>
      pWindowStdLatency += plot((1 to wl.length).map { _.toDouble }, wl.map { _._2 })
      // pWindowStdLatency += plot((1 to windowLatency.length).map { _.toDouble }, windowLatency.map { _ => stdLatency })
    }


    val pWindowStdInterval = f.subplot(pRow, pCol, 3)
    pWindowStdInterval.title = "Windowed Std of Interval"
    windowInterval foreach { wi =>
      pWindowStdInterval += plot((1 to wi.length).map { _.toDouble }, wi.map { _._2 })
      // pWindowStdInterval += plot((1 to windowInterval.length).map { _.toDouble }, windowInterval.map { _ => stdInterval })
    }

    val pWindowMDSLatency = f.subplot(pRow, pCol, 4)
    pWindowMDSLatency.title = "Windowed Coefficient of Variation of Latency"
    windowLatency foreach { wl =>
      pWindowMDSLatency +=  plot((1 to wl.length).map { _. toDouble }, wl.map { e => e._2 / e._1 })
    }

    val pWindowMDSInterval = f.subplot(pRow, pCol, 5)
    pWindowMDSInterval.title = "Windowed Coefficient of Variation of Interval"
    windowInterval foreach { wi =>
      pWindowMDSInterval +=  plot((1 to wi.length).map { _. toDouble }, wi.map { e => e._2 / e._1 })
    }

    val pWindowVMDSLatency = f.subplot(pRow, pCol, 6)
    pWindowVMDSLatency.title = "Windowed var/mean"
    windowLatency foreach { wl =>
      pWindowVMDSLatency +=  plot((1 to wl.length).map { _. toDouble }, wl.map { e => e._2 * e._2 / e._1 })
    }

    val pWindowVMDSInterval = f.subplot(pRow, pCol, 7)
    pWindowVMDSInterval.title = "Windowed var/mean"
    windowInterval foreach { wi =>
      pWindowVMDSInterval +=  plot((1 to wi.length).map { _. toDouble }, wi.map { e => e._2 * e._2  / e._1 })
    }

    println(s"latency, mean: $meanLatency, std: $stdLatency")
    println(s"interval, mean: $meanInterval, std: $stdInterval")

    val firstFile = agg.head
    val path = firstFile.substring(0, firstFile.lastIndexOf('/'))
    f.saveas(s"$path/$outputFilename-wind.png")
    f2.saveas(s"$path/$outputFilename-pdf.png")

    /*
    val mean_latency = latency_filtered.sum / latency_filtered.length
    val stderr_latency = math.sqrt(latency_filtered.map(e => e*e).sum / latency_filtered.length - mean_latency * mean_latency)
    println(s"validate latency, mean: $mean_latency, std: $stderr_latency")
    */

  }

  def meanArray[T](arr: Array[T])(implicit num: Fractional[T]): T = {
    import num._
    arr.sum / num.fromInt(arr.length)
  }

  def stderrArray[T](arr: Array[T])(implicit num: Fractional[T]): Double = {
    import num._
    val mean = meanArray(arr)
    val err = arr.map(e => e*e).sum / num.fromInt(arr.length) - mean * mean
    math.sqrt(err.toDouble())
  }

  def windowArray(arr: Array[Double], windowSize: Int): Array[(Double, Double)] = {
    arr.sliding(windowSize).toArray map { a =>
      (meanArray(a), stderrArray(a))
    }
  }
}
