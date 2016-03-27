object HealthChecker {

  val indentation = "\t\t"

  def printlnWithIndentation(s: String) {
    println(indentation + s)
  }
  def main(args: Array[String]) = {
    println("HealthChecker starts...")
    println("--------------------------------------------------")
    if (args.length < 2) {
      println("USAGE:  [property file] [full | rt | static | cons]")
      System.exit(1)
    }
    println("Using property file: " + Config.LIGHTBLUE + args(0) + Config.NOCOLOUR)
    Config.readPropFile(args(0))

    val mode = args(1)

    //--------------------------------------------------
    val thdDBChecker = new Thread(new Runnable {
      def run() {

        //--------------------------------------------------
        // add symbols in the portfolios table to symbolsToChk
        //--------------------------------------------------
        Config.addSymToChk(DBProcessor.getSymFromPortfoliosTbl)
        //--------------------------------------------------

        //--------------------------------------------------
        if (mode == "static" || mode == "full") {
          print(Config.LIGHTGREEN)
          println("daily_hsi_price:")
          print(Config.NOCOLOUR)
          val (ts, p) = DBProcessor.checkDailyHSITbl
          println(indentation + "HSI timestamp: " + ts + " [" + p + "]")
          print(Config.LIGHTGREEN)
          println("daily_pnl")
          print(Config.NOCOLOUR)
          DBProcessor.checkPnL("daily_pnl", -10, 0).foreach(s => println(indentation + s))
          print(Config.LIGHTGREEN)
          println("market_data_daily_hk_stock")
          print(Config.NOCOLOUR)
          DBProcessor.checkMktData("market_data_daily_hk_stock", "close").foreach(printlnWithIndentation)
          print(Config.LIGHTGREEN)
          println("market_data_hourly_hk_stock")
          print(Config.NOCOLOUR)
          DBProcessor.checkMktData("market_data_hourly_hk_stock", "close").foreach(printlnWithIndentation)
          print(Config.LIGHTGREEN)
          println("trades:")
          print(Config.NOCOLOUR)
          DBProcessor.getTrades.foreach(printlnWithIndentation)
          print(Config.LIGHTGREEN)
          println("portfolios:")
          print(Config.NOCOLOUR)
          DBProcessor.getPort.foreach(printlnWithIndentation)

          print(Config.LIGHTGREEN)
          println("trading_account:")
          print(Config.NOCOLOUR)
          DBProcessor.getTradingAc.foreach(printlnWithIndentation)
        }

        if (mode == "rt" || mode == "full") {
          print(Config.YELLOW)
          println("intraday_pnl")
          print(Config.NOCOLOUR)
          DBProcessor.checkPnL("intraday_pnl", 0, -5).foreach(s => println(indentation + s))
          print(Config.YELLOW)
          println("intraday_pnl_per_strategy")
          print(Config.NOCOLOUR)
          DBProcessor.checkIntradayPnLPerSty.foreach(s => println(indentation + s))
          print(Config.YELLOW)
          println("market_data_intraday")
          print(Config.NOCOLOUR)
          DBProcessor.checkMktData("market_data_intraday", "nominal_price").foreach(printlnWithIndentation)

        }

        if (mode == "cons" || mode == "full") {
          print(Config.LIGHTCYAN)
          println("Consistency trades vs portfolios")
          print(Config.NOCOLOUR)

          if (DBProcessor.checkTradesAgstPort) {
            print(Config.LIGHTGREEN)
            printlnWithIndentation("PASS")
          }
          else {
            print(Config.LIGHTRED)
            printlnWithIndentation("FAIL")
          }
          print(Config.NOCOLOUR)

          print(Config.LIGHTCYAN)
          println("Consistency trades vs trading_account")
          print(Config.NOCOLOUR)

          if (DBProcessor.checkTradesAgstTradingAc) {
            print(Config.LIGHTGREEN)
            printlnWithIndentation("PASS")
          }
          else {
            print(Config.LIGHTRED)
            printlnWithIndentation("FAIL")
          }
          print(Config.NOCOLOUR)

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
