package com.example

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.content.PartData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.*
import kotlin.collections.ArrayList

object User : Table("user") {
    val id = com.example.User.integer("id")
    val name = com.example.User.varchar("name", length = 50)
    val email = com.example.User.varchar("email", length = 50)
}
data class Users(val id: Int, val name: String, val email: String)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        gson {
        }
    }
    initDB()




    val client = HttpClient(Apache) {
    }



    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/1") {
            call.respondText(getTopuserData(), ContentType.Text.Plain)
        }

        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.red
                }
                p {
                    fontSize = 2.em
                }
                rule("p.myclass") {
                    color = Color.blue
                }
            }
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }

        post("/savebooks") {
            val multipart = call.receive<String>()
            call.respond(multipart+"csdc")
            /*call.respondTextWriter {
                if (!call.request.isMultipart()) {
                    appendln("Not a multipart request")
                } else {
                    while (true) {
                        val part = multipart.readPart() ?: break
                        when (part) {
                            is PartData.FormItem ->
                                appendln("FormItem: ${part.name} = ${part.value}")
                            is PartData.FileItem ->
                                appendln("FileItem: ${part.name} -> ${part.originalFileName} of ${part.contentType}")
                        }
                        part.dispose()
                    }
                }
            }*/
        }
    }
}

fun initDB() {
    val connectionProps = Properties()
    connectionProps.put("user", "root")
    connectionProps.put("password", "root")

    try {
        Class.forName("com.mysql.cj.jdbc.Driver").newInstance()
        val conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mytestdb?useUnicode=true&serverTimezone=UTC&useSSL=false", connectionProps)

        var stmt: Statement? = null
        var resultset: ResultSet? = null

        stmt = conn!!.createStatement()
        resultset = stmt!!.executeQuery("SHOW DATABASES;")

        if (stmt.execute("SHOW DATABASES;")) {
            resultset = stmt.resultSet
        }

        while (resultset!!.next()) {
            println(resultset.getString("Database"))
        }


    } catch (ex: SQLException) {
        // handle any errors
        ex.printStackTrace()
    } catch (ex: Exception) {
        // handle any errors
        ex.printStackTrace()
    }
}
fun getTopuserData(): String {
    var json: String = ""
    transaction {
        val res = User.selectAll().orderBy(User.id, false).limit(5)
        val c = ArrayList<Users>()
        for (f in res) {
            c.add(Users(id = f[User.id], name = f[User.name], email = f[User.email]))
        }
        json = Gson().toJson(c);
    }
    return json
}

fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

fun CommonAttributeGroupFacade.style(builder: CSSBuilder.() -> Unit) {
    this.style = CSSBuilder().apply(builder).toString().trim()
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
