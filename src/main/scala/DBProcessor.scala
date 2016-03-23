import org.nirvana._
import java.sql.{Connection, DriverManager, ResultSet, Timestamp};
import java.util.Properties;
import scala.collection.mutable.ListBuffer
import org.joda.time.{Period, DateTime, Duration}

object DBProcessor {
  //--------------------------------------------------
  // mysql
  //--------------------------------------------------
  val delimiter: String = "\t|\t"

  Class.forName("com.mysql.jdbc.Driver")

  var lsConn = List[Connection]()

  (0 until Config.jdbcConnStr.length).foreach(i => {
    var p = new Properties()
    p.put("user", Config.jdbcUser(i))
    p.put("password", Config.jdbcPwd(i))
    val _conn = DriverManager.getConnection(Config.jdbcConnStr(i), p)
    _conn.setAutoCommit(false)
    lsConn ::= _conn
  })

  def checkDailyHSITbl(): (String, Double) = {
    val _conn = lsConn.head

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val prep = _conn.prepareStatement("select timestamp,price from daily_hsi_price order by timestamp desc limit 1")
      val rs = prep.executeQuery()

      if (rs.next) {
        (rs.getString("timestamp"), rs.getDouble("price"))
      }
      else {
        ("N/A", 0.0)
      }
    }
  }

  def checkPnL(tbl: String, daysShift: Int, secondsShift: Int): List[String] = {
    val _conn = lsConn.head

    var resultLs = List[String]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val prep = _conn.prepareStatement("select timestamp,sum(realized_pnl) as a,sum(unrealized_pnl) as b,strategy_id from " + tbl + " where timestamp >= ? group by timestamp,strategy_id order by strategy_id desc,timestamp desc")
      prep.setString(1, SUtil.convertDateTimeToStr(SUtil.getCurrentDateTime(HongKong()).plusDays(daysShift).plusSeconds(secondsShift)))

      val rs = prep.executeQuery()

      while (rs.next) {
        resultLs = resultLs :+ rs.getString("timestamp") + delimiter + rs.getDouble("a").toLong.toString + delimiter + rs.getDouble("b").toLong.toString + delimiter + rs.getString("strategy_id")
      }
    }

    resultLs
  }

  def checkIntradayPnLPerSty(): List[String] = {
    val _conn = lsConn.head

    var resultLs = List[String]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val prep = _conn.prepareStatement("select timestamp,total_pnl,strategy_id from intraday_pnl_per_strategy where timestamp >= ? order by strategy_id desc,timestamp desc")
      prep.setString(1, SUtil.convertDateTimeToStr(SUtil.getCurrentDateTime(HongKong()).plusSeconds(-10)))

      val rs = prep.executeQuery()

      while (rs.next) {
        resultLs = resultLs :+ rs.getString("timestamp") + delimiter + rs.getDouble("total_pnl").toLong.toString + delimiter + rs.getString("strategy_id")
      }
    }

    resultLs
  }

  def checkMktData(tbl: String, field: String): List[String] = {
    val _conn = lsConn.head

    var resultLs = List[String]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      Config.symbolsToChk.foreach(x => {
        val prep = _conn.prepareStatement("select timestamp,instrument_id," + field + " from " + tbl + " where instrument_id = ? order by timestamp desc limit 1")
        prep.setString(1, x)
        val rs = prep.executeQuery()

        while (rs.next) {
          resultLs = resultLs :+ rs.getString("timestamp") + delimiter + rs.getString("instrument_id") + delimiter + rs.getDouble(field)
        }
      })
    }

    resultLs
  }

  def getPort(): List[String] = {
    val _conn = lsConn.head

    var resultLs = List[String]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val prep = _conn.prepareStatement("select timestamp,strategy_id,instrument_id,volume,avg_price,unrealized_pnl,market_value from portfolios")
      val rs = prep.executeQuery()

      resultLs = resultLs :+ "timestamp" + delimiter + "strategy_id" + delimiter + "instrument_id" + delimiter + "volume" + delimiter + "avg_price" + delimiter + "unrealized_pnl" + delimiter + "market_value"

      while (rs.next) {
        resultLs = resultLs :+ rs.getString("timestamp") + delimiter + rs.getString("strategy_id") + delimiter + rs.getString("instrument_id") + delimiter + rs.getDouble("volume") + delimiter + rs.getDouble("avg_price") + delimiter + rs.getString("unrealized_pnl") + delimiter + rs.getString("market_value")
      }
    }

    resultLs
  }

  def getTrades(): List[String] = {
    val _conn = lsConn.head

    var resultLs = List[String]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val prep = _conn.prepareStatement("select timestamp,instrument_id,trade_price,trade_volume,buy_sell,strategy_id from trades")

      val rs = prep.executeQuery()

      resultLs = resultLs :+ "timestamp" + delimiter + "instrument_id" + delimiter + "trade_price" + delimiter + "trade_volume" + delimiter + "buy_sell" + delimiter + "strategy_id"

      while (rs.next) {
        resultLs = resultLs :+ rs.getString("timestamp") + delimiter + rs.getString("instrument_id") + delimiter + rs.getDouble("trade_price") + delimiter + rs.getDouble("trade_volume") + delimiter + rs.getInt("buy_sell") + delimiter + rs.getString("strategy_id")
      }
    }

    resultLs
  }

  def checkTradesAgstPort(): Boolean = {
    val _conn = lsConn.head

    var l1 = List[String]()
    var l2 = List[String]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val prep1 = _conn.prepareStatement("select instrument_id, sum(svol) as volume, strategy_id from (select timestamp,instrument_id,trade_volume * if(buy_sell=1, 1, -1) as svol,strategy_id from trades) as sub group by strategy_id, instrument_id order by strategy_id asc, instrument_id asc")
      val rs1 = prep1.executeQuery()

      while (rs1.next) {
        if (Math.abs(rs1.getDouble("volume")) > 0) {
          l1 = l1 :+ rs1.getString("instrument_id")
          l1 = l1 :+ delimiter
          l1 = l1 :+ rs1.getDouble("volume").toString
          l1 = l1 :+ delimiter
          l1 = l1 :+ rs1.getString("strategy_id")
        }
      }

      val prep2 = _conn.prepareStatement("select instrument_id, volume, strategy_id from portfolios order by strategy_id asc, instrument_id asc")
      val rs2 = prep2.executeQuery()

      while (rs2.next) {
        l2 = l2 :+ rs2.getString("instrument_id")
        l2 = l2 :+ delimiter
        l2 = l2 :+ rs2.getDouble("volume").toString
        l2 = l2 :+ delimiter
        l2 = l2 :+ rs2.getString("strategy_id")
      }

    }

    l1.zip(l2).forall(x => x._1 == x._2)

  }

  def getTradingAc(): List[String] = {
    val _conn = lsConn.head

    var resultLs = List[String]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val prep = _conn.prepareStatement("select timestamp,cash,avail_cash,holding_cash,strategy_id from trading_account")

      val rs = prep.executeQuery()

      resultLs = resultLs :+ "timestamp" + delimiter + "cash" + delimiter + "avail_cash" + delimiter + "holding_cash" + delimiter + "strategy_id"

      while (rs.next) {
        resultLs = resultLs :+ rs.getString("timestamp") + delimiter + rs.getDouble("cash").toString + delimiter + rs.getDouble("avail_cash").toString + delimiter + rs.getDouble("holding_cash").toString + delimiter + rs.getString("strategy_id")
      }
    }

    resultLs
  }

  def checkTradesAgstTradingAc(): Boolean = {
    val _conn = lsConn.head

    var l1 = List[Double]()
    var l2 = List[Double]()
    var l3 = List[(Double, Double, Double)]()

    try {
      val statement = _conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      Config.sty.foreach(x => {
        val prep1 = _conn.prepareStatement("select sum(cashflow) as cashflow, strategy_id from (select trade_price * trade_volume * if(buy_sell=1, 1, -1) as cashflow,strategy_id from trades) as sub where strategy_id = ? group by strategy_id")
        prep1.setString(1, x)
        val rs1 = prep1.executeQuery()

        if (rs1.next) {
          l1 = l1 :+ rs1.getDouble("cashflow")
        }
        else {
          l1 = l1 :+ 0.0
        }
      })

      Config.sty.foreach(x => {
        val prep2 = _conn.prepareStatement("select cash,avail_cash,holding_cash from trading_account where strategy_id = ?")
        prep2.setString(1, x)
        val rs2 = prep2.executeQuery()

        if (rs2.next) {
          l2 = l2 :+ rs2.getDouble("cash")
          l3 = l3 :+ (rs2.getDouble("cash"), rs2.getDouble("avail_cash"), rs2.getDouble("holding_cash"))
        }
        else {
          l2 = l2 :+ Config.initialCapital
        }
      })
    }

//--------------------------------------------------
// the actual checking:
// 1. trades vs trading_account
// 2. trading_account cash = avail_cash + holding_cash
//--------------------------------------------------
    l1.zip(l2).forall(x => Math.abs((Config.initialCapital - x._1) - x._2) < Config.EPSILON) &&
      l3.forall(x => Math.abs(x._1 - x._2 - x._3) < Config.EPSILON)

  }

  def closeConn(): Unit = {
    lsConn.foreach(_.close)
  }

}

