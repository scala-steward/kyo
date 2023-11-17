package kyo.chatgpt

import kyo._
import kyo.ios._
import kyo.locals._
import kyo.chatgpt.ais.AIs

object configs {

  case class Model(name: String, maxTokens: Int)

  object Model {
    val gpt4            = Model("gpt-4", 8192)
    val gpt4_turbo      = Model("gpt-4-1106-preview", 128000)
    val gpt4_vision     = Model("gpt-4-vision-preview", 128000)
    val gpt4_32k        = Model("gpt-4-32k", 32768)
    val gpt35_turbo     = Model("gpt-3.5-turbo", 4097)
    val gpt35_turbo_16k = Model("gpt-3.5-turbo-16k", 16385)
  }

  case class Config(
      apiUrl: String,
      apiKey: Option[String],
      model: Model,
      temperature: Double,
      maxTokens: Option[Int],
      seed: Option[Int]
  ) {
    def apiUrl(url: String): Config =
      copy(apiUrl = url)
    def apiKey(key: String): Config =
      copy(apiKey = Some(key))
    def model(model: Model): Config =
      copy(model = model)
    def temperature(temperature: Double): Config =
      copy(temperature = temperature.max(0).min(2))
    def maxTokens(maxTokens: Option[Int]): Config =
      copy(maxTokens = maxTokens)
    def seed(seed: Option[Int]): Config =
      copy(seed = seed)
  }

  object Config {
    val default = {
      val apiKeyProp = "OPENAI_API_KEY"
      val apiKey =
        Option(System.getenv(apiKeyProp))
          .orElse(Option(System.getProperty(apiKeyProp)))
      Config("https://api.openai.com", apiKey, Model.gpt4_turbo, 0.2, None, None)
    }
  }

  object Configs {

    private val local = Locals.init(Config.default)

    def get: Config > IOs =
      local.get

    def apiKey: String > IOs =
      get.map(_.apiKey.getOrElse(IOs.fail[String]("Can't locate the OpenAI API key")))

    def let[T, S](f: Config => Config)(v: T > S): T > (IOs with S) =
      local.get.map { c =>
        local.let(f(c))(v)
      }
  }
}