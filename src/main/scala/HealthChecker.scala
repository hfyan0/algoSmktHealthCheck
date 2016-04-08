import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.joda.time.format.DateTimeFormat

object HealthChecker {

  val indentation = "\t\t"

  def printlnWithIndentation(s: String) {
    println(indentation + s)
  }

  def printPassOrFail(b: Boolean) {
    if (b) {
      print(Config.LIGHTGREEN)
      println("PASS")
    }
    else {
      print(Config.LIGHTRED)
      println("FAIL")
    }
    print(Config.NOCOLOUR)
  }

  def printlnWithColour(s: String, sColour: String, width: Int) {
    printWithColour(s, sColour, width)
    println()
  }
  def printWithColour(s: String, sColour: String, width: Int) {
    print(sColour)
    if (s.length > width) {
      print(s.substring(0, width))
    }
    else {
      print(s)
      (1 to width - s.length).foreach(x => print("."))
    }
    print(Config.NOCOLOUR)
  }

  def main(args: Array[String]) = {
    println("HealthChecker starts...")
    println("--------------------------------------------------")
    if (args.length < 2) {
      println("USAGE:  [property file] [full|rt|static|consistency] [yyyy-mm-dd (previous trading day)] [detail|nodetail] [continuous|onetime]")
      System.exit(1)
    }
    println("Using property file: " + Config.LIGHTBLUE + args(0) + Config.NOCOLOUR)
    Config.readPropFile(args(0))

    val mode = args(1)

    val prevTrdgDay = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(args(2))
    println("Previous trading day: " + prevTrdgDay)

    val detailMode = if (args(3) == "detail") true else false
    val continuousMode = if (args(4) == "continuous") true else false

    //--------------------------------------------------
    val thdDBChecker = new Thread(new Runnable {
      def run() {

        val noOfTimesToLoop = (if (continuousMode) 99999999 else 1)
        var loopCount = 0
        while (loopCount < noOfTimesToLoop) {

          println("Using property file: " + Config.LIGHTBLUE + args(0) + Config.NOCOLOUR)
          //--------------------------------------------------
          // add symbols in the portfolios table to symbolsToChk
          //--------------------------------------------------
          Config.addSymToChk(DBProcessor.getSymFromPortfoliosTbl)
          //--------------------------------------------------

          //--------------------------------------------------
          if (mode == "static" || mode == "full") {

            {
              printWithColour("daily_hsi_price:", Config.LIGHTPURPLE, Config.textWidth)
              val (b, ts, p) = DBProcessor.checkDailyHSITbl(prevTrdgDay)
              printPassOrFail(b)
              if (detailMode)
                println(indentation + "HSI timestamp: " + ts + " [" + p + "]")
            }

            {
              printWithColour("daily_pnl", Config.LIGHTPURPLE, Config.textWidth)
              val (b, ls) = DBProcessor.checkPnL("daily_pnl", -1, 0)
              printPassOrFail(b)
              if (detailMode)
                ls.foreach(printlnWithIndentation)
            }

            {
              printWithColour("market_data_daily_hk_stock", Config.LIGHTPURPLE, Config.textWidth)
              val (b, ls) = DBProcessor.checkMktData("market_data_daily_hk_stock", "close", prevTrdgDay)
              printPassOrFail(b)
              if (detailMode)
                ls.foreach(printlnWithIndentation)
            }

            {
              printWithColour("market_data_hourly_hk_stock", Config.LIGHTPURPLE, Config.textWidth)
              val (b, ls) = DBProcessor.checkMktData("market_data_hourly_hk_stock", "close", prevTrdgDay)
              printPassOrFail(b)
              if (detailMode)
                ls.foreach(printlnWithIndentation)
            }

            if (detailMode) {
              printWithColour("trades:", Config.LIGHTPURPLE, Config.textWidth)
              DBProcessor.getTrades.foreach(printlnWithIndentation)

              printWithColour("portfolios:", Config.LIGHTPURPLE, Config.textWidth)
              DBProcessor.getPort.foreach(printlnWithIndentation)

              printWithColour("trading_account:", Config.LIGHTPURPLE, Config.textWidth)
              DBProcessor.getTradingAc.foreach(printlnWithIndentation)
            }
          }

          if (mode == "rt" || mode == "full") {

            {
              printWithColour("intraday_pnl", Config.YELLOW, Config.textWidth)
              val (b, ls) = DBProcessor.checkPnL("intraday_pnl", 0, -5)
              printPassOrFail(b)
              if (detailMode)
                ls.foreach(printlnWithIndentation)
            }

            {
              printWithColour("intraday_pnl_per_strategy", Config.YELLOW, Config.textWidth)
              val (b, ls) = DBProcessor.checkIntradayPnLPerSty
              printPassOrFail(b)
              if (detailMode)
                ls.foreach(printlnWithIndentation)
            }

            {
              printWithColour("market_data_intraday", Config.YELLOW, Config.textWidth)
              val (b, ls) = DBProcessor.checkMktData("market_data_intraday", "nominal_price", prevTrdgDay)
              printPassOrFail(b)
              if (detailMode)
                ls.foreach(printlnWithIndentation)
            }

          }

          if (mode == "consistency" || mode == "full") {
            printWithColour("Consistency trades vs portfolios", Config.LIGHTCYAN, Config.textWidth)
            printPassOrFail(DBProcessor.checkTradesAgstPort)

            {
              printWithColour("Consistency trades vs trading_account", Config.LIGHTCYAN, Config.textWidth)
              val (b, ls) = DBProcessor.checkTradesAgstTradingAc
              printPassOrFail(b)
              if (detailMode)
                ls.foreach(printlnWithIndentation)
            }

            {
              printWithColour("Consistency signals vs trades", Config.LIGHTCYAN, Config.textWidth)
              val (b, ls1) = DBProcessor.checkSignalAgstTrades
              printPassOrFail(b)
              if (detailMode)
                ls1.foreach(println)
            }

            {
              printWithColour("Consistency orders vs trades", Config.LIGHTCYAN, Config.textWidth)
              val (b, ls1) = DBProcessor.checkOrdersAgstTrades
              printPassOrFail(b)
              if (detailMode)
                ls1.foreach(println)
            }

            {
              printWithColour("Consistency orders itself", Config.LIGHTCYAN, Config.textWidth)
              val (b, ls1) = DBProcessor.checkOrders
              printPassOrFail(b)
              if (detailMode)
                ls1.foreach(println)
            }

            {
              printWithColour("Consistency trading_account", Config.LIGHTCYAN, Config.textWidth)
              printPassOrFail(DBProcessor.checkTradingAc1)
            }

          }

          if (continuousMode) {
            (1 to Config.sleepTime / 1000).foreach(
              x => {
                if (x > 11)
                  print((Config.sleepTime / 1000 - x + 1).toString)
                else
                  print(".")
                Thread.sleep(1000)
              }
            )
            println
          }

          loopCount += 1

        }

        //--------------------------------------------------

      }
    })

    //--------------------------------------------------
    thdDBChecker.start

    thdDBChecker.join
    // while (true) {
    //   Thread.sleep(10000);
    // }
    //--------------------------------------------------

  }
}
