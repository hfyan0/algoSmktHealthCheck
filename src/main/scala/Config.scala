import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;
import org.joda.time.{DateTime, LocalDate, LocalTime}

object Config {

  val BLACK = "\033[0;30m"
  val BLUE = "\033[0;34m"
  val GREEN = "\033[0;32m"
  val CYAN = "\033[0;36m"
  val RED = "\033[0;31m"
  val PURPLE = "\033[0;35m"
  val BROWN = "\033[0;33m"
  val LIGHTGRAY = "\033[0;37m"
  val DARKGRAY = "\033[1;30m"
  val LIGHTBLUE = "\033[1;34m"
  val LIGHTGREEN = "\033[1;32m"
  val LIGHTCYAN = "\033[1;36m"
  val LIGHTRED = "\033[1;31m"
  val LIGHTPURPLE = "\033[1;35m"
  val YELLOW = "\033[1;33m"
  val WHITE = "\033[1;37m"
  val NOCOLOUR = "\033[0m"
  val BLUEBKGDWHITE = "\033[1;37m\033[44m"
  val HIGHLIGHT1 = "\033[1;36m\033[40m"
  val GREYBKGDYELLOW = "\033[1;33m\033[40m"
  val GREYBKGDLIGHTRED = "\033[1;31m\033[40m"
  val GREYBKGDWHITE = "\033[1;37m\033[40m"

  def readPropFile(propFileName: String) {

    try {
      val prop = new Properties()
      prop.load(new FileInputStream(propFileName))

      (1 to 9).foreach(i => {
        val connStr = prop.getProperty("jdbcConnStr" + i)
        if (connStr != "" && connStr != null) {
          jdbcConnStr ::= connStr
          jdbcUser ::= prop.getProperty("jdbcUser" + i)
          jdbcPwd ::= prop.getProperty("jdbcPwd" + i)
        }
      })

      initialCapital = prop.getProperty("initialCapital").toDouble

    }
    catch {
      case e: Exception =>
        {
          e.printStackTrace()
          sys.exit(1)
        }
    }
  }

  //--------------------------------------------------
  // JDBC
  //--------------------------------------------------
  var jdbcConnStr = List[String]()
  var jdbcUser = List[String]()
  var jdbcPwd = List[String]()

  var symbolsToChk: Set[String] = Set("00941", "00005", "00388")
  var initialCapital = 0.0

  var EPSILON = 1.0
  val sty: Set[String] = Set("B2_HK","s1")
}
