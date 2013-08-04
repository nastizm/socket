package com.example.socket
import java.io.{BufferedReader,InputStreamReader,BufferedWriter,OutputStreamWriter,IOException}
import java.net.{ServerSocket,Socket,SocketException}
import scala.util.control.Breaks
import scala.util.matching.Regex
import scala.Array

object App {
  def main(args: Array[String]) {
    // apacheが担ってる部分
    val listener = new ServerSocket(8080)

    // listnerはユーザーが飛ばしてきたリクエストを検知する。
    // 検知されるまでは待機し、リクエストがきたらサブプロセスに処理を依頼する。
    while (true) new ServerThread(listener.accept()).start()
  }
}

case class ServerThread(socket: Socket) extends Thread("SampleTrhead") {
  override def run(): Unit = {
    try {

      val in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
      val out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
      val b = new Breaks

      // ヘッダー部の処理
      val firstReg:Regex = """([A-Z]+)\s+(.+)\s(.+)""".r
      val headerReg:Regex = """(.+):(.+)""".r;
      val firstLine = in.readLine()
      var method = ""
      var action = ""
      firstLine match {
        case firstReg(m, a, _) => {
          method = m
          action = a
        }
        case _ => println("最初の行のマッチに失敗" + firstLine)
      }
      println("method=" + method + ":action=" + action)
      println("リクエストヘッダー全文")
      val headerMap = {
        val hm = Map.newBuilder[String, String]
        b.breakable {
          while(true) {
            val line = in.readLine()
            // ヘッダー行解析
            line match {
              case headerReg(title, value) => {
                hm += title -> value.trim
                println(title+":"+value)
              }
              case _ => false
            }
            if (line == "") b.break()
          }
        }
        hm.result
      }

      // リクエスト本文取得
      if (!headerMap.get("Content-Length").isEmpty) {
        val len = headerMap.get("Content-Length").get.trim.toInt
        var l = new Array[Char](len)
        in.read(l, 0, len)
        println("")
        l.foreach {x => print(x)}
        println("")
      }


      // レスポンスを返そう!
      val body = """<html><haed></head><body><form action="/action" method="POST"><input type="text" name="name"><input type="submit" value="send"></form></body></html>"""
      val contentLength = body.length
      val res = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: " + contentLength + "\r\n\r\n" + body
      out.write(res)
      out.flush()

      in.close()
      out.close()
      socket.close()
    } catch {
      case e: SocketException =>()
      case e: IOException => e.printStackTrace()
    }
  }
}