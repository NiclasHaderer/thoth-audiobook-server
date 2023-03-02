package io.thoth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

data class Agents(val agents: List<Any>)

fun main() {

  // load file with jackson
  val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
  // read file contents of ./test-config.json
  val content = Files.readString(Path.of("./test-config.json"))
  val agents = objectMapper.readValue<Agents>(content)
  println(agents)
}
