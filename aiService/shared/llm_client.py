import logging
import os
from abc import ABC, abstractmethod

from shared.settings import LLMConfig, config

logger = logging.getLogger(__name__)


class BaseLLMClient(ABC):

    provider_name: str = "unknown"
    model_name: str = "unknown"

    @abstractmethod
    def generate(self, prompt: str, system_prompt: str = "", response_json: bool = False) -> str:
        """프롬프트를 입력받아 모델 응답 텍스트를 생성

        response_json=True 이면 순수 JSON 문자열 응답을 요청함
        (지원하지 않는 provider는 이 옵션을 무시하고 일반 텍스트로 응답)
        """
        raise NotImplementedError

    @abstractmethod
    def is_available(self) -> bool:
        raise NotImplementedError


class GeminiClient(BaseLLMClient):
    # Gemini 사용
    def __init__(self, llm_config: LLMConfig):
        """설정을 받아 Gemini 모델 초기화를 시도합니다."""
        self.cfg = llm_config
        self.provider_name = "gemini"
        self.model_name = llm_config.model_name
        self._model = None
        self._setup()

    def generate(self, prompt: str, system_prompt: str = "", response_json: bool = False) -> str:
        # 프롬포트 전달 및 응답 받음
        if not self._model:
            return "[LLM not connected - check API key]"
        full_prompt = f"{system_prompt}\n\n{prompt}" if system_prompt else prompt

        generation_config = None
        if response_json:
            import google.generativeai as genai

            generation_config = genai.GenerationConfig(
                temperature=self.cfg.temperature,
                max_output_tokens=self.cfg.max_tokens,
                response_mime_type="application/json",
            )

        response = self._model.generate_content(full_prompt, generation_config=generation_config)
        return response.text

    def is_available(self) -> bool:
        return self._model is not None

    def _setup(self):
        try:
            import google.generativeai as genai

            api_key = os.environ.get(self.cfg.api_key_env)
            if not api_key:
                logger.warning(
                    "Environment variable '%s' not found. "
                    "Set it in Colab before running the pipeline.",
                    self.cfg.api_key_env,
                )
                return
            genai.configure(api_key=api_key)
            generation_config = genai.GenerationConfig(
                temperature=self.cfg.temperature,
                max_output_tokens=self.cfg.max_tokens,
            )
            self._model = genai.GenerativeModel(
                model_name=self.cfg.model_name,
                generation_config=generation_config,
            )
            logger.info("Gemini initialized: %s", self.cfg.model_name)
        except ImportError:
            logger.error("google-generativeai is not installed.")


class OpenAIClient(BaseLLMClient):
    def __init__(self, llm_config: LLMConfig):
        self.cfg = llm_config
        self.provider_name = "openai"
        self.model_name = llm_config.model_name
        self._client = None
        self._setup()

    def generate(self, prompt: str, system_prompt: str = "", response_json: bool = False) -> str:
        if not self._client:
            return "[LLM not connected]"
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})
        extra_kwargs = {}
        if response_json:
            extra_kwargs["response_format"] = {"type": "json_object"}
        response = self._client.chat.completions.create(
            model=self.cfg.model_name,
            messages=messages,
            temperature=self.cfg.temperature,
            max_tokens=self.cfg.max_tokens,
            **extra_kwargs,
        )
        return response.choices[0].message.content

    def is_available(self) -> bool:
        return self._client is not None

    def _setup(self):
        try:
            from openai import OpenAI

            api_key = os.environ.get("OPENAI_API_KEY")
            if not api_key:
                logger.warning("OPENAI_API_KEY environment variable not found.")
                return
            self._client = OpenAI(api_key=api_key)
            logger.info("OpenAI initialized: %s", self.cfg.model_name)
        except ImportError:
            logger.error("openai is not installed.")


class LocalHFClient(BaseLLMClient):
    # 로컬 Hugging Face 생성 모델을 사용하는 클라이언트

    def __init__(self, llm_config: LLMConfig):
        # 설정을 받아 로컬 모델 파이프라인 초기화를 시도합
        self.cfg = llm_config
        self.provider_name = "local_hf"
        self.model_name = llm_config.model_name
        self._pipeline = None
        self._setup()

    def generate(self, prompt: str, system_prompt: str = "", response_json: bool = False) -> str:
        if not self._pipeline:
            return "[Local LLM not ready]"
        formatted = (
            f"<s>[INST] {system_prompt}\n\n{prompt} [/INST]"
            if system_prompt
            else f"<s>[INST] {prompt} [/INST]"
        )
        result = self._pipeline(formatted)
        generated = result[0]["generated_text"]
        return generated.split("[/INST]")[-1].strip()

    def is_available(self) -> bool:
        return self._pipeline is not None

    def _setup(self):

        try:
            import torch
            from transformers import AutoModelForCausalLM, AutoTokenizer, pipeline

            logger.info("Loading local model: %s", self.cfg.model_name)
            tokenizer = AutoTokenizer.from_pretrained(self.cfg.model_name)
            model = AutoModelForCausalLM.from_pretrained(
                self.cfg.model_name,
                torch_dtype=torch.float16,
                device_map="auto",
                load_in_4bit=True,
            )
            self._pipeline = pipeline(
                "text-generation",
                model=model,
                tokenizer=tokenizer,
                max_new_tokens=self.cfg.max_tokens,
                temperature=self.cfg.temperature,
                do_sample=True,
            )
            logger.info("Local model ready: %s", self.cfg.model_name)
        except Exception as exc:
            logger.error("Failed to load local model: %s", exc)


def get_llm_provider_name(llm: BaseLLMClient) -> str:
    return getattr(llm, "provider_name", llm.__class__.__name__)


def get_llm_model_name(llm: BaseLLMClient) -> str:
    return getattr(llm, "model_name", llm.__class__.__name__)


def build_llm_client(llm_config: LLMConfig = None) -> BaseLLMClient:
    # 설정된 provider 이름에 맞는 LLM 클라이언트를 생성합

    cfg = llm_config or config.llm
    providers = {
        "gemini": GeminiClient,
        "openai": OpenAIClient,
        "local_hf": LocalHFClient,
    }
    client_cls = providers.get(cfg.provider)
    if not client_cls:
        raise ValueError(f"Unknown LLM provider: {cfg.provider}")
    return client_cls(cfg)